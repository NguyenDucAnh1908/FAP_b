param(
    [string]$BaseUrl = "http://localhost:8080/api/v1",
    [string]$MaterialFile = (Join-Path $PSScriptRoot "..\test-data\files\sample-material.txt"),
    [string]$InvalidFile = (Join-Path $PSScriptRoot "..\test-data\files\invalid-file-type.exe")
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Net.Http
$BaseUrl = $BaseUrl.TrimEnd("/")
$MaterialFile = (Resolve-Path $MaterialFile).Path
$InvalidFile = (Resolve-Path $InvalidFile).Path
$oversizedFile = $null
$runId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds().ToString()
$today = Get-Date
$futureDate = $today.Date.AddDays(14)
$classEnd = $today.Date.AddDays(90)
$script:httpClient = [System.Net.Http.HttpClient]::new()
$script:httpClient.Timeout = [TimeSpan]::FromSeconds(30)

function New-ApiRequest {
    param(
        [string]$Method,
        [string]$Path,
        [string]$Token
    )

    $request = [System.Net.Http.HttpRequestMessage]::new(
        [System.Net.Http.HttpMethod]::new($Method),
        "$BaseUrl$Path"
    )
    $request.Headers.AcceptLanguage.ParseAdd("vi")
    if ($Token) {
        $request.Headers.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $Token)
    }
    return $request
}

function Read-ApiResponse {
    param(
        [string]$Name,
        [System.Net.Http.HttpResponseMessage]$Response,
        [int[]]$ExpectedStatus
    )

    $status = [int]$Response.StatusCode
    $raw = $Response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    if ($ExpectedStatus -notcontains $status) {
        throw "$Name failed with HTTP $status. Response: $raw"
    }

    Write-Host ("[PASS] {0} -> HTTP {1}" -f $Name, $status)
    if ([string]::IsNullOrWhiteSpace($raw)) {
        return $null
    }
    try {
        return $raw | ConvertFrom-Json
    } catch {
        return $raw
    }
}

function Invoke-Api {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Path,
        [object]$Body,
        [string]$Token,
        [int[]]$ExpectedStatus = @(200)
    )

    $request = New-ApiRequest -Method $Method -Path $Path -Token $Token
    if ($PSBoundParameters.ContainsKey("Body")) {
        $json = $Body | ConvertTo-Json -Depth 20 -Compress
        $request.Content = [System.Net.Http.StringContent]::new(
            $json,
            [System.Text.Encoding]::UTF8,
            "application/json"
        )
    }

    try {
        $response = $script:httpClient.SendAsync($request).GetAwaiter().GetResult()
        try {
            return Read-ApiResponse -Name $Name -Response $response -ExpectedStatus $ExpectedStatus
        } finally {
            $response.Dispose()
        }
    } finally {
        $request.Dispose()
    }
}

function Invoke-FileUpload {
    param(
        [string]$Name,
        [string]$Path,
        [string]$FilePath,
        [string]$ContentType,
        [string]$Token,
        [int[]]$ExpectedStatus = @(201)
    )

    $request = New-ApiRequest -Method "POST" -Path $Path -Token $Token
    $multipart = [System.Net.Http.MultipartFormDataContent]::new()
    $fileContent = [System.Net.Http.ByteArrayContent]::new([System.IO.File]::ReadAllBytes($FilePath))
    $fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::new($ContentType)
    $multipart.Add($fileContent, "file", [System.IO.Path]::GetFileName($FilePath))
    $request.Content = $multipart

    try {
        $response = $script:httpClient.SendAsync($request).GetAwaiter().GetResult()
        try {
            return Read-ApiResponse -Name $Name -Response $response -ExpectedStatus $ExpectedStatus
        } finally {
            $response.Dispose()
        }
    } finally {
        $request.Dispose()
    }
}

function Invoke-EmptyMultipartUpload {
    param(
        [string]$Name,
        [string]$Path,
        [string]$Token,
        [int[]]$ExpectedStatus = @(400)
    )

    $request = New-ApiRequest -Method "POST" -Path $Path -Token $Token
    $multipart = [System.Net.Http.MultipartFormDataContent]::new()
    $multipart.Add([System.Net.Http.StringContent]::new("missing-file"), "description")
    $request.Content = $multipart
    try {
        $response = $script:httpClient.SendAsync($request).GetAwaiter().GetResult()
        try {
            return Read-ApiResponse -Name $Name -Response $response -ExpectedStatus $ExpectedStatus
        } finally {
            $response.Dispose()
        }
    } finally {
        $request.Dispose()
    }
}

function Assert-DownloadedFile {
    param(
        [long]$MaterialId,
        [string]$Token,
        [string]$ExpectedFile
    )

    $request = New-ApiRequest -Method "GET" -Path "/materials/$MaterialId/download" -Token $Token
    try {
        $response = $script:httpClient.SendAsync($request).GetAwaiter().GetResult()
        try {
            if ([int]$response.StatusCode -ne 200) {
                $raw = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
                throw "Download material failed with HTTP $([int]$response.StatusCode). Response: $raw"
            }
            $actual = $response.Content.ReadAsByteArrayAsync().GetAwaiter().GetResult()
            $expected = [System.IO.File]::ReadAllBytes($ExpectedFile)
            $sha = [System.Security.Cryptography.SHA256]::Create()
            try {
                $actualHash = [System.BitConverter]::ToString($sha.ComputeHash($actual))
                $expectedHash = [System.BitConverter]::ToString($sha.ComputeHash($expected))
            } finally {
                $sha.Dispose()
            }
            if ($actualHash -ne $expectedHash) {
                throw "Downloaded material content does not match the uploaded file."
            }
            Write-Host "[PASS] Download material and SHA-256 integrity -> HTTP 200"
        } finally {
            $response.Dispose()
        }
    } finally {
        $request.Dispose()
    }
}

try {
    Write-Host "Running FAP E2E flow against $BaseUrl (runId=$runId)"

    $adminLogin = Invoke-Api -Name "Login Super Admin" -Method "POST" -Path "/auth/login" -Body @{
        email = "e2e.superadmin@fap.local"
        password = "Password@123"
    }
    $adminToken = $adminLogin.data.accessToken

    $roles = Invoke-Api -Name "Load roles" -Method "GET" -Path "/roles" -Token $adminToken
    $classAdminRoleId = ($roles.data | Where-Object { $_.name -eq "Class Admin" }).id
    $trainerRoleId = ($roles.data | Where-Object { $_.name -eq "Trainer" }).id
    $traineeRoleId = ($roles.data | Where-Object { $_.name -eq "Trainee" }).id
    if (!$classAdminRoleId -or !$trainerRoleId -or !$traineeRoleId) {
        throw "Required roles were not returned by GET /roles."
    }

    $classAdmin = Invoke-Api -Name "Create Class Admin" -Method "POST" -Path "/users" -Token $adminToken -ExpectedStatus @(201) -Body @{
        fullName = "API Flow Class Admin $runId"
        email = "flow.classadmin.$runId@fap.local"
        phone = "0903000001"
        password = "Password@123"
        dateOfBirth = "1990-01-15"
        gender = "Male"
        roleIds = @($classAdminRoleId)
    }
    $trainer = Invoke-Api -Name "Create Trainer" -Method "POST" -Path "/users" -Token $adminToken -ExpectedStatus @(201) -Body @{
        fullName = "API Flow Trainer $runId"
        email = "flow.trainer.$runId@fap.local"
        phone = "0903000002"
        password = "Password@123"
        dateOfBirth = "1988-03-20"
        gender = "Female"
        roleIds = @($trainerRoleId)
    }
    $trainee = Invoke-Api -Name "Create Trainee" -Method "POST" -Path "/users" -Token $adminToken -ExpectedStatus @(201) -Body @{
        fullName = "API Flow Trainee $runId"
        email = "flow.trainee.$runId@fap.local"
        phone = "0903000003"
        password = "Password@123"
        dateOfBirth = "2001-07-10"
        gender = "Male"
        roleIds = @($traineeRoleId)
    }

    $syllabusPayload = @{
        name = "API Flow Java Backend $runId"
        code = "FLOW-JAVA-$runId"
        version = "v1.0"
        levelName = "Intermediate"
        attendees = 20
        duration = "40 hours"
        technicalRequirements = "JDK 21, IntelliJ IDEA, Oracle XE"
        courseObjectives = "Build and secure a Spring Boot REST API."
        rules = "Attendance >= 80%; final score >= 60%."
        timeAllocAssignmentLab = 50
        timeAllocConceptLecture = 30
        timeAllocGuideReview = 10
        timeAllocTestQuiz = 10
        assessQuizPct = 20
        assessAssignmentPct = 30
        assessFinalPct = 50
        assessmentText = "Quiz 20%; Assignment 30%; Final 50%."
        outputStandards = @("H4SD", "K6SD")
        days = @(@{
            dayNumber = 1
            sortOrder = 1
            units = @(@{
                name = "REST API foundations"
                sortOrder = 1
                topics = @(@{
                    name = "Controller, validation, and error handling"
                    outputStandard = "H4SD"
                    online = $true
                    durationMinutes = 120
                    status = "Active"
                    sortOrder = 1
                    materials = @()
                })
            })
        })
    }
    $syllabus = Invoke-Api -Name "Create full syllabus" -Method "POST" -Path "/syllabuses/full" -Token $adminToken -ExpectedStatus @(201) -Body $syllabusPayload
    $syllabusId = $syllabus.data.syllabus.id
    $topicId = $syllabus.data.days[0].units[0].topics[0].id

    $material = Invoke-FileUpload -Name "Upload valid material" -Path "/materials/upload?syllabusId=$syllabusId&topicId=$topicId" -FilePath $MaterialFile -ContentType "text/plain" -Token $adminToken
    $materialId = $material.data.id
    Invoke-FileUpload -Name "Reject invalid material type" -Path "/materials/upload?syllabusId=$syllabusId&topicId=$topicId" -FilePath $InvalidFile -ContentType "application/octet-stream" -Token $adminToken -ExpectedStatus @(400) | Out-Null
    Invoke-EmptyMultipartUpload -Name "Reject missing multipart file" -Path "/materials/upload?syllabusId=$syllabusId&topicId=$topicId" -Token $adminToken | Out-Null
    $oversizedFile = Join-Path ([System.IO.Path]::GetTempPath()) "fap-oversized-$runId.txt"
    $oversizedStream = [System.IO.File]::Open($oversizedFile, [System.IO.FileMode]::CreateNew)
    try {
        $oversizedStream.SetLength((20 * 1024 * 1024) + 1)
    } finally {
        $oversizedStream.Dispose()
    }
    Invoke-FileUpload -Name "Reject oversized material" -Path "/materials/upload?syllabusId=$syllabusId&topicId=$topicId" -FilePath $oversizedFile -ContentType "text/plain" -Token $adminToken -ExpectedStatus @(400) | Out-Null
    Invoke-Api -Name "Return unknown material as not found" -Method "GET" -Path "/materials/999999999999/download" -Token $adminToken -ExpectedStatus @(404) | Out-Null

    Invoke-Api -Name "Submit syllabus" -Method "PATCH" -Path "/syllabuses/$syllabusId/status" -Token $adminToken -Body @{ status = "Pending" } | Out-Null
    Invoke-Api -Name "Activate syllabus" -Method "PATCH" -Path "/syllabuses/$syllabusId/status" -Token $adminToken -Body @{ status = "Active" } | Out-Null

    $program = Invoke-Api -Name "Create training program" -Method "POST" -Path "/training-programs" -Token $adminToken -ExpectedStatus @(201) -Body @{
        name = "API Flow Training Program $runId"
        duration = "12 weeks"
        totalHours = 240
        version = "v1.0"
    }
    $programId = $program.data.id
    Invoke-Api -Name "Attach syllabus to program" -Method "PUT" -Path "/training-programs/$programId/syllabuses" -Token $adminToken -Body @{
        syllabuses = @(@{ syllabusId = $syllabusId; sortOrder = 1 })
    } | Out-Null
    Invoke-Api -Name "Activate program" -Method "PATCH" -Path "/training-programs/$programId/status" -Token $adminToken -Body @{ status = "Active" } | Out-Null

    $class = Invoke-Api -Name "Create class" -Method "POST" -Path "/classes" -Token $adminToken -ExpectedStatus @(201) -Body @{
        name = "API Flow Java Class $runId"
        classCode = "FLOW-CLASS-$runId"
        trainingProgramId = $programId
        location = "Ho Chi Minh City"
        locationDetail = "F-Town 3, Room A101"
        fsu = "FHM"
        classTime = "09:00 - 11:00"
        startDate = $today.ToString("yyyy-MM-dd")
        endDate = $classEnd.ToString("yyyy-MM-dd")
        duration = "12 weeks"
    }
    $classId = $class.data.id
    Invoke-Api -Name "Assign Class Admin" -Method "PUT" -Path "/classes/$classId/admins" -Token $adminToken -Body @{ userIds = @($classAdmin.data.id) } | Out-Null
    Invoke-Api -Name "Assign Trainer" -Method "PUT" -Path "/classes/$classId/trainers" -Token $adminToken -Body @{
        trainers = @(@{ userId = $trainer.data.id; syllabusId = $syllabusId })
    } | Out-Null
    Invoke-Api -Name "Activate class" -Method "PATCH" -Path "/classes/$classId/status" -Token $adminToken -Body @{ status = "Active" } | Out-Null

    $session = Invoke-Api -Name "Create training session" -Method "POST" -Path "/training-sessions" -Token $adminToken -ExpectedStatus @(201) -Body @{
        classId = $classId
        title = "API Flow REST Workshop $runId"
        description = "End-to-end session created by verify-e2e-flow.ps1."
        trainerId = $trainer.data.id
        room = "A101"
        sessionDate = $futureDate.ToString("yyyy-MM-dd")
        startTime = $futureDate.ToString("yyyy-MM-dd") + "T09:00:00"
        endTime = $futureDate.ToString("yyyy-MM-dd") + "T11:00:00"
        sessionType = "Hybrid"
        meetingLink = "https://meet.example.com/api-flow-$runId"
        capacity = 20
    }
    $sessionId = $session.data.id

    $question = Invoke-Api -Name "Create question" -Method "POST" -Path "/questions" -Token $adminToken -ExpectedStatus @(201) -Body @{
        content = "API Flow: Which annotation validates a request body? $runId"
        questionType = "single"
        category = "API Flow"
        difficulty = "Easy"
        optionsJson = @("@Valid", "@Bean", "@Table", "@Async")
        correctAnswersJson = @("@Valid")
        explanation = "Jakarta Validation is triggered by @Valid."
    }
    $quiz = Invoke-Api -Name "Create quiz" -Method "POST" -Path "/quizzes" -Token $adminToken -ExpectedStatus @(201) -Body @{
        title = "API Flow Java Readiness $runId"
        description = "Quiz for the end-to-end API flow."
        durationMinutes = 20
        passingScore = 60
        maxAttempts = 2
        randomize = $false
        category = "API Flow"
        openDate = $today.ToString("yyyy-MM-dd")
        closeDate = $classEnd.ToString("yyyy-MM-dd")
    }
    $quizId = $quiz.data.id
    Invoke-Api -Name "List questions sorted by createdAt" -Method "GET" -Path "/questions?limit=20&sortBy=createdAt&order=desc" -Token $adminToken | Out-Null
    Invoke-Api -Name "List quizzes sorted by createdAt" -Method "GET" -Path "/quizzes?limit=20&sortBy=createdAt&order=desc" -Token $adminToken | Out-Null
    Invoke-Api -Name "Attach question to quiz" -Method "PUT" -Path "/quizzes/$quizId/questions" -Token $adminToken -Body @{
        questions = @(@{ questionId = $question.data.id; sortOrder = 1; points = 1.0 })
    } | Out-Null
    Invoke-Api -Name "Publish quiz" -Method "PATCH" -Path "/quizzes/$quizId/status" -Token $adminToken -Body @{ status = "Published" } | Out-Null
    Invoke-Api -Name "Assign quiz" -Method "POST" -Path "/quizzes/$quizId/assignments" -Token $adminToken -ExpectedStatus @(201) -Body @{
        trainingSessionId = $sessionId
    } | Out-Null

    $traineeLogin = Invoke-Api -Name "Login created Trainee" -Method "POST" -Path "/auth/login" -Body @{
        email = "flow.trainee.$runId@fap.local"
        password = "Password@123"
    }
    $traineeToken = $traineeLogin.data.accessToken
    Invoke-Api -Name "Register Trainee" -Method "POST" -Path "/training-sessions/$sessionId/registrations" -Token $traineeToken | Out-Null
    Assert-DownloadedFile -MaterialId $materialId -Token $traineeToken -ExpectedFile $MaterialFile

    $attempt = Invoke-Api -Name "Start quiz attempt" -Method "POST" -Path "/quizzes/$quizId/attempts" -Token $traineeToken -ExpectedStatus @(201)
    $attemptId = $attempt.data.id
    Invoke-Api -Name "Save quiz answer" -Method "PUT" -Path "/quiz-attempts/$attemptId/answers" -Token $traineeToken -Body @{
        answers = @(@{ questionId = $question.data.id; selectedAnswersJson = @("@Valid") })
    } | Out-Null
    Invoke-Api -Name "Submit quiz attempt" -Method "POST" -Path "/quiz-attempts/$attemptId/submit" -Token $traineeToken | Out-Null
    Invoke-Api -Name "Review quiz result" -Method "GET" -Path "/quiz-attempts/$attemptId/review" -Token $traineeToken | Out-Null

    Invoke-Api -Name "QR check-in" -Method "POST" -Path "/training-sessions/$sessionId/check-in" -Token $traineeToken | Out-Null
    Invoke-Api -Name "Complete session" -Method "PATCH" -Path "/training-sessions/$sessionId/status" -Token $adminToken -Body @{ status = "Completed" } | Out-Null
    Invoke-Api -Name "Submit feedback" -Method "POST" -Path "/training-sessions/$sessionId/feedback" -Token $traineeToken -ExpectedStatus @(201) -Body @{
        ratingContent = 5
        ratingTrainer = 5
        ratingOrganization = 4
        comment = "The automated API flow completed successfully."
    } | Out-Null
    Invoke-Api -Name "Verify Trainee dashboard" -Method "GET" -Path "/me/training-dashboard" -Token $traineeToken | Out-Null
    Invoke-Api -Name "Verify quiz result list" -Method "GET" -Path "/quizzes/$quizId/attempts?page=1&limit=20&sortBy=score&order=desc" -Token $adminToken | Out-Null
    Invoke-Api -Name "Verify audit trail" -Method "GET" -Path "/audit-logs?userId=$($trainee.data.id)&page=1&limit=20" -Token $adminToken | Out-Null

    Invoke-Api -Name "Reject missing token" -Method "GET" -Path "/users" -ExpectedStatus @(401) | Out-Null
    Invoke-Api -Name "Reject Trainee user listing" -Method "GET" -Path "/users" -Token $traineeToken -ExpectedStatus @(403) | Out-Null
    Invoke-Api -Name "Reject invalid user DTO" -Method "POST" -Path "/users" -Token $adminToken -ExpectedStatus @(422) -Body @{
        fullName = ""
        email = "not-an-email"
        password = "weak"
        dateOfBirth = "2099-01-01"
        gender = "Male"
        roleIds = @()
    } | Out-Null
    Invoke-Api -Name "Return unknown user as not found" -Method "GET" -Path "/users/999999999" -Token $adminToken -ExpectedStatus @(404) | Out-Null
    Invoke-Api -Name "Reject duplicate email" -Method "POST" -Path "/users" -Token $adminToken -ExpectedStatus @(409) -Body @{
        fullName = "Duplicate API Flow User"
        email = "flow.trainee.$runId@fap.local"
        password = "Password@123"
        dateOfBirth = "2000-01-01"
        gender = "Male"
        roleIds = @($traineeRoleId)
    } | Out-Null
    Invoke-Api -Name "Reject unknown sort field" -Method "GET" -Path "/users?sortBy=notAField&order=asc" -Token $adminToken -ExpectedStatus @(400) | Out-Null
    Invoke-Api -Name "Reject invalid completed-session transition" -Method "PATCH" -Path "/training-sessions/$sessionId/status" -Token $adminToken -ExpectedStatus @(409) -Body @{ status = "Upcoming" } | Out-Null

    Write-Host ""
    Write-Host "FAP E2E flow completed successfully."
    Write-Host "Run ID: $runId"
    Write-Host "Created class: FLOW-CLASS-$runId"
    Write-Host "Created Trainee: flow.trainee.$runId@fap.local"
    Write-Host "Use scripts/reset-dev-data.sql to remove seed and flow data in local development."
} finally {
	$script:httpClient.Dispose()
	if ($oversizedFile -and (Test-Path -LiteralPath $oversizedFile)) {
		Remove-Item -LiteralPath $oversizedFile
	}
}
