// AUTO-GENERATED FRONTEND API CONTRACT FOR FAP BACKEND
// Source: Spring MVC controllers and DTO records under src/main/java/com/fap
// Generated: 2026-06-01
// Copy this file into the FE project to type API requests/responses and endpoint metadata.

export const API_BASE_PATH = "/api/v1" as const;
export type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
export type ISODate = string;
export type ISODateTime = string;

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

export interface PageResponse<T> {
  success: boolean;
  data: T[];
  pagination: { page: number; limit: number; total: number; totalPages: number };
}

export interface ErrorResponse {
  success: false;
  code: string;
  message: string;
  details?: { field: string; message: string }[];
}

export interface ApiEndpoint {
  key: string;
  tag: string;
  summary: string;
  method: HttpMethod;
  path: string;
  auth: boolean;
  pathParams?: readonly ApiParam[];
  query?: readonly ApiParam[];
  request?: string;
  formData?: readonly ApiParam[];
  response: string;
}

export interface ApiParam {
  name: string;
  type: string;
  required?: boolean;
  default?: string;
}

export type AttendanceCheckInMethod =
  | "Manual"
  | "QR";

export type AttendanceStatus =
  | "Present"
  | "Late"
  | "Absent";

export type ClassStatus =
  | "Planning"
  | "Active"
  | "Closed";

export type Gender =
  | "Male"
  | "Female";

export type PermissionLevel =
  | "access_denied"
  | "view"
  | "create"
  | "modify"
  | "full_access";

export type QuestionDifficulty =
  | "Easy"
  | "Medium"
  | "Hard";

export type QuestionType =
  | "single"
  | "multiple";

export type QuizAttemptStatus =
  | "InProgress"
  | "Submitted";

export type QuizStatus =
  | "Draft"
  | "Published"
  | "Closed";

export type SyllabusStatus =
  | "Drafting"
  | "Pending"
  | "Active"
  | "Inactive";

export type SyllabusTopicStatus =
  | "Active"
  | "Inactive";

export type TrainingProgramStatus =
  | "Planning"
  | "Active"
  | "Inactive";

export type TrainingRegistrationStatus =
  | "Registered"
  | "Waitlist"
  | "Completed"
  | "Cancelled";

export type TrainingSessionStatus =
  | "Upcoming"
  | "Completed"
  | "Canceled";

export type TrainingSessionType =
  | "Offline"
  | "Online"
  | "Hybrid";

export type UserStatus =
  | "Active"
  | "Inactive";

export interface AssignedMaterialFileResponse {
  id: number;
  syllabusId: number;
  syllabusName: string;
  syllabusCode: string;
  topicId: number;
  topicName: string;
  fileName: string;
  fileUrl: string;
  fileSize: number;
  contentType: string;
  uploadedBy: number;
  uploadedAt: ISODateTime;
}

export interface AssignedQuizResponse {
  id: number;
  title: string;
  description: string;
  durationMinutes: number;
  passingScore: number;
  maxAttempts: number;
  category: string;
  openDate: ISODate;
  closeDate: ISODate;
  questionCount: long;
  attemptCount: long;
  remainingAttempts: long;
  latestAttemptId: number;
  latestAttemptStatus: QuizAttemptStatus;
  latestScore: number;
  latestPassed: boolean;
}

export interface AttendanceItemRequest {
  userId: number;
  status: AttendanceStatus;
  checkedInAt?: ISODateTime;
  checkInMethod?: AttendanceCheckInMethod;
  correctionReason?: string;
}

export interface AttendanceProgress {
  present?: long;
  late?: long;
  absent?: long;
}

export interface AttendanceRecordResponse {
  id: number;
  trainingSessionId: number;
  userId: number;
  userFullName: string;
  userEmail: string;
  status: AttendanceStatus;
  checkedInAt: ISODateTime;
  checkInMethod: AttendanceCheckInMethod;
  updatedBy: number;
  correctionReason: string;
  createdAt: ISODateTime;
  updatedAt: ISODateTime;
}

export interface AttendanceSummary {
  present?: long;
  late?: long;
  absent?: long;
}

export interface AuditLogResponse {
  id: number;
  userId: number;
  action: string;
  entityType: string;
  entityId: number;
  ipAddress: string;
  createdAt: ISODateTime;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: long;
  user: UserResponse;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface ClassAdminResponse {
  userId: number;
  userFullName: string;
  userEmail: string;
}

export interface ClassResponse {
  id: number;
  name: string;
  classCode: string;
  trainingProgramId: number;
  trainingProgramName: string;
  status: ClassStatus;
  location: string;
  locationDetail: string;
  fsu: string;
  classTime: string;
  startDate: ISODate;
  endDate: ISODate;
  duration: string;
  createdBy: number;
  updatedBy: number;
  createdAt: ISODateTime;
  updatedAt: ISODateTime;
}

export interface ClassTrainerItemRequest {
  userId: number;
  syllabusId?: number;
}

export interface ClassTrainerResponse {
  id: number;
  userId: number;
  userFullName: string;
  userEmail: string;
  syllabusId: number;
  syllabusName: string;
  syllabusCode: string;
}

export interface CreateClassRequest {
  name: string;
  classCode: string;
  trainingProgramId: number;
  location?: string;
  locationDetail?: string;
  fsu?: string;
  classTime?: string;
  startDate?: ISODate;
  endDate?: ISODate;
  duration?: string;
}

export interface CreateMaterialFileRequest {
  fileName: string;
  fileUrl: string;
  fileSize?: number;
  contentType?: string;
}

export interface CreateMaterialRequest {
  syllabusId: number;
  topicId: number;
  fileName: string;
  fileUrl: string;
  fileSize?: number;
  contentType?: string;
}

export interface CreateQuestionRequest {
  content: string;
  questionType: QuestionType;
  category: string;
  difficulty: QuestionDifficulty;
  optionsJson: JsonNode;
  correctAnswersJson: JsonNode;
  explanation?: string;
}

export interface CreateQuizAssignmentRequest {
  classId?: number;
  trainingSessionId?: number;
}

export interface CreateQuizRequest {
  title: string;
  description?: string;
  durationMinutes: number;
  passingScore: number;
  maxAttempts: number;
  randomize?: boolean;
  category: string;
  openDate?: ISODate;
  closeDate?: ISODate;
}

export interface CreateSyllabusDayRequest {
  dayNumber: number;
  sortOrder: number;
}

export interface CreateSyllabusRequest {
  name: string;
  code: string;
  version: string;
  levelName: string;
  attendees: number;
  duration?: string;
  technicalRequirements?: string;
  courseObjectives?: string;
  rules?: string;
  timeAllocAssignmentLab: number;
  timeAllocConceptLecture: number;
  timeAllocGuideReview: number;
  timeAllocTestQuiz: number;
  assessQuizPct: number;
  assessAssignmentPct: number;
  assessFinalPct: number;
  assessmentText?: string;
}

export interface CreateSyllabusTopicRequest {
  name: string;
  outputStandard: string;
  online: boolean;
  durationMinutes: number;
  status: SyllabusTopicStatus;
  sortOrder: number;
}

export interface CreateSyllabusUnitRequest {
  name: string;
  sortOrder: number;
}

export interface CreateTrainingFeedbackRequest {
  ratingContent: number;
  ratingTrainer: number;
  ratingOrganization: number;
  comment?: string;
}

export interface CreateTrainingProgramRequest {
  name: string;
  duration?: string;
  totalHours?: number;
  version: string;
}

export interface CreateTrainingSessionRequest {
  classId: number;
  title: string;
  description?: string;
  trainerId: number;
  room?: string;
  sessionDate: ISODate;
  startTime: ISODateTime;
  endTime: ISODateTime;
  sessionType: TrainingSessionType;
  meetingLink?: string;
  capacity: number;
}

export interface CreateUserRequest {
  fullName: string;
  email: string;
  phone?: string;
  password: string;
  dateOfBirth?: ISODate;
  gender: Gender;
  avatarUrl?: string;
  roleIds: number[];
}

export interface ErrorBody {
  code?: string;
  message?: string;
  details?: FieldError[];
}

export interface FieldError {
  field?: string;
  message?: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface GoogleLoginRequest {
  idToken: string;
}

export interface ImportError {
  row?: number;
  field?: string;
  message?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LogoutRequest {
  refreshToken: string;
}

export interface MaterialFileResponse {
  id: number;
  topicId: number;
  fileName: string;
  fileUrl: string;
  fileSize: number;
  contentType: string;
  uploadedBy: number;
  uploadedAt: ISODateTime;
}

export interface MaterialProgress {
  total?: long;
}

export interface MyAttendanceResponse {
  attendanceId: number;
  attendanceStatus: AttendanceStatus;
  checkedInAt: ISODateTime;
  checkInMethod: AttendanceCheckInMethod;
  correctionReason: string;
  createdAt: ISODateTime;
  updatedAt: ISODateTime;
  trainingSessionId: number;
  trainingSessionTitle: string;
  trainingSessionStatus: TrainingSessionStatus;
  sessionType: TrainingSessionType;
  sessionDate: ISODate;
  startTime: ISODateTime;
  endTime: ISODateTime;
  room: string;
  meetingLink: string;
  classId: number;
  className: string;
  classCode: string;
  trainerId: number;
  trainerFullName: string;
  trainerEmail: string;
}

export interface MyClassAdminDashboardResponse {
  assignedClasses: long;
  activeClasses: long;
  planningClasses: long;
  upcomingSessions: long;
  pendingAttendanceSessions: long;
  totalTrainers: long;
  totalParticipants: long;
  classesStartingSoon: ClassResponse[];
  recentSessions: TrainingSessionResponse[];
}

export interface MyClassDetailResponse {
  classInfo: ClassResponse;
  syllabuses: MyClassSyllabusResponse[];
}

export interface MyClassLearningContentResponse {
  classInfo: ClassResponse;
  syllabuses: MyClassSyllabusResponse[];
  sessions: MyTrainingSessionResponse[];
  materials: AssignedMaterialFileResponse[];
  quizzes: AssignedQuizResponse[];
}

export interface MyClassProgressResponse {
  classInfo: ClassResponse;
  sessions: SessionProgress;
  attendance: AttendanceProgress;
  materials: MaterialProgress;
  quizzes: QuizProgress;
}

export interface MyClassSyllabusResponse {
  syllabusId: number;
  name: string;
  code: string;
  version: string;
  status: SyllabusStatus;
  levelName: string;
  duration: string;
  sortOrder: number;
}

export interface MyTrainerDashboardResponse {
  assignedClasses: long;
  upcomingSessions: long;
  completedSessions: long;
  pendingAttendanceSessions: long;
  nextSessions: TrainingSessionResponse[];
  recentCompletedSessions: TrainingSessionResponse[];
}

export interface MyTrainingDashboardResponse {
  registeredSessions: long;
  upcomingSessions: long;
  completedSessions: long;
  waitlistedSessions: long;
  attendanceSummary: AttendanceSummary;
  nextSessions: MyTrainingSessionResponse[];
  recentAttendance: MyAttendanceResponse[];
}

export interface MyTrainingRegistrationResponse {
  registrationId: number;
  registrationStatus: TrainingRegistrationStatus;
  registeredAt: ISODateTime;
  cancelledAt: ISODateTime;
  completedAt: ISODateTime;
  trainingSessionId: number;
  trainingSessionTitle: string;
  trainingSessionStatus: TrainingSessionStatus;
  sessionType: TrainingSessionType;
  sessionDate: ISODate;
  startTime: ISODateTime;
  endTime: ISODateTime;
  room: string;
  meetingLink: string;
  classId: number;
  className: string;
  classCode: string;
  trainerId: number;
  trainerFullName: string;
  trainerEmail: string;
}

export interface MyTrainingSessionResponse {
  trainingSessionId: number;
  title: string;
  description: string;
  status: TrainingSessionStatus;
  sessionType: TrainingSessionType;
  sessionDate: ISODate;
  startTime: ISODateTime;
  endTime: ISODateTime;
  room: string;
  meetingLink: string;
  capacity: number;
  enrolledCount: number;
  classId: number;
  className: string;
  classCode: string;
  trainerId: number;
  trainerFullName: string;
  trainerEmail: string;
  registrationId: number;
  registrationStatus: TrainingRegistrationStatus;
  registeredAt: ISODateTime;
  cancelledAt: ISODateTime;
  completedAt: ISODateTime;
}

export interface NotificationResponse {
  id: number;
  title: string;
  message: string;
  read: boolean;
  createdAt: ISODateTime;
}

export interface Pagination {
  page?: number;
  limit?: number;
  total?: long;
  totalPages?: number;
}

export interface PermissionResponse {
  roleId: number;
  roleName: string;
  resource: string;
  permissionLevel: PermissionLevel;
}

export interface QuestionResponse {
  id: number;
  content: string;
  questionType: QuestionType;
  category: string;
  difficulty: QuestionDifficulty;
  optionsJson: JsonNode;
  correctAnswersJson: JsonNode;
  explanation: string;
  createdBy: number;
  updatedBy: number;
  createdAt: ISODateTime;
  updatedAt: ISODateTime;
}

export interface QuizAnswerItemRequest {
  questionId: number;
  selectedAnswersJson: JsonNode;
}

export interface QuizAssignmentResponse {
  id: number;
  quizId: number;
  quizTitle: string;
  classId: number;
  className: string;
  classCode: string;
  trainingSessionId: number;
  trainingSessionTitle: string;
  assignedBy: number;
  assignedByName: string;
  assignedByEmail: string;
  assignedAt: ISODateTime;
}

export interface QuizAttemptQuestionResponse {
  questionId: number;
  sortOrder: number;
  points: number;
  content: string;
  questionType: QuestionType;
  category: string;
  difficulty: QuestionDifficulty;
  optionsJson: JsonNode;
}

export interface QuizAttemptResponse {
  id: number;
  quizId: number;
  quizTitle: string;
  attemptNumber: number;
  status: QuizAttemptStatus;
  answersJson: JsonNode;
  score: number;
  correctCount: number;
  totalQuestions: number;
  passed: boolean;
  timeTakenSeconds: number;
  startedAt: ISODateTime;
  submittedAt: ISODateTime;
  questions: QuizAttemptQuestionResponse[];
}

export interface QuizAttemptResultResponse {
  id: number;
  quizId: number;
  quizTitle: string;
  userId: number;
  userFullName: string;
  userEmail: string;
  attemptNumber: number;
  status: QuizAttemptStatus;
  score: number;
  correctCount: number;
  totalQuestions: number;
  passed: boolean;
  timeTakenSeconds: number;
  startedAt: ISODateTime;
  submittedAt: ISODateTime;
}

export interface QuizAttemptReviewQuestionResponse {
  questionId: number;
  sortOrder: number;
  points: number;
  content: string;
  questionType: QuestionType;
  category: string;
  difficulty: QuestionDifficulty;
  optionsJson: JsonNode;
  selectedAnswersJson: JsonNode;
  correctAnswersJson: JsonNode;
  correct: boolean;
  explanation: string;
}

export interface QuizAttemptReviewResponse {
  id: number;
  quizId: number;
  quizTitle: string;
  attemptNumber: number;
  status: QuizAttemptStatus;
  answersJson: JsonNode;
  score: number;
  correctCount: number;
  totalQuestions: number;
  passed: boolean;
  timeTakenSeconds: number;
  startedAt: ISODateTime;
  submittedAt: ISODateTime;
  questions: QuizAttemptReviewQuestionResponse[];
}

export interface QuizAttemptSummaryResponse {
  quizId: number;
  quizTitle: string;
  totalAttempts: long;
  inProgressAttempts: long;
  submittedAttempts: long;
  passedAttempts: long;
  failedAttempts: long;
  passRate: number;
  averageScore: number;
  highestScore: number;
  lowestScore: number;
}

export interface QuizProgress {
  assigned?: long;
  attempted?: long;
  passed?: long;
  remaining?: long;
  latestAttemptId?: number;
  latestScore?: number;
  latestPassed?: boolean;
}

export interface QuizQuestionItemRequest {
  questionId: number;
  sortOrder: number;
  points: number;
}

export interface QuizQuestionResponse {
  questionId: number;
  sortOrder: number;
  points: number;
  question: QuestionResponse;
}

export interface QuizResponse {
  id: number;
  title: string;
  description: string;
  durationMinutes: number;
  passingScore: number;
  maxAttempts: number;
  randomize: boolean;
  category: string;
  status: QuizStatus;
  openDate: ISODate;
  closeDate: ISODate;
  questionCount: long;
  createdBy: number;
  updatedBy: number;
  createdAt: ISODateTime;
  updatedAt: ISODateTime;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface ResetPasswordRequest {
  otp: string;
  newPassword: string;
}

export interface RoleResponse {
  id: number;
  name: string;
  description: string;
}

export interface SaveQuizAnswersRequest {
  answers: QuizAnswerItemRequest[];
}

export interface SessionProgress {
  total?: long;
  completed?: long;
  upcoming?: long;
  canceled?: long;
}

export interface SettingsResponse {
  settings: Record<string, Record<string, string>>;
}

export interface SyllabusDayResponse {
  id: number;
  dayNumber: number;
  sortOrder: number;
  units: SyllabusUnitResponse[];
}

export interface SyllabusImportResponse {
  totalRows: number;
  successCount: number;
  failedCount: number;
  errors: ImportError[];
  createdSyllabuses: SyllabusResponse[];
}

export interface SyllabusResponse {
  id: number;
  name: string;
  code: string;
  version: string;
  status: SyllabusStatus;
  levelName: string;
  attendees: number;
  duration: string;
  technicalRequirements: string;
  courseObjectives: string;
  rules: string;
  timeAllocAssignmentLab: number;
  timeAllocConceptLecture: number;
  timeAllocGuideReview: number;
  timeAllocTestQuiz: number;
  assessQuizPct: number;
  assessAssignmentPct: number;
  assessFinalPct: number;
  assessmentText: string;
  createdBy: number;
  updatedBy: number;
  createdAt: ISODateTime;
  updatedAt: ISODateTime;
}

export interface SyllabusTopicResponse {
  id: number;
  name: string;
  outputStandard: string;
  online: boolean;
  durationMinutes: number;
  status: SyllabusTopicStatus;
  sortOrder: number;
}

export interface SyllabusUnitResponse {
  id: number;
  name: string;
  sortOrder: number;
  topics: SyllabusTopicResponse[];
}

export interface TrainingFeedbackResponse {
  id: number;
  trainingSessionId: number;
  trainingSessionTitle: string;
  userId: number;
  userFullName: string;
  userEmail: string;
  ratingContent: number;
  ratingTrainer: number;
  ratingOrganization: number;
  comment: string;
  createdAt: ISODateTime;
  updatedAt: ISODateTime;
}

export interface TrainingFeedbackSummaryResponse {
  trainingSessionId: number;
  feedbackCount: long;
  averageContentRating: number;
  averageTrainerRating: number;
  averageOrganizationRating: number;
  overallAverageRating: number;
}

export interface TrainingParticipantsResponse {
  trainingSessionId: number;
  capacity: number;
  enrolledCount: number;
  registered: TrainingRegistrationResponse[];
  waitlist: TrainingRegistrationResponse[];
}

export interface TrainingProgramResponse {
  id: number;
  name: string;
  status: TrainingProgramStatus;
  duration: string;
  totalHours: number;
  version: string;
  createdBy: number;
  updatedBy: number;
  createdAt: ISODateTime;
  updatedAt: ISODateTime;
}

export interface TrainingProgramSyllabusItemRequest {
  syllabusId: number;
  sortOrder: number;
}

export interface TrainingProgramSyllabusResponse {
  syllabusId: number;
  syllabusName: string;
  syllabusCode: string;
  syllabusVersion: string;
  syllabusStatus: SyllabusStatus;
  sortOrder: number;
}

export interface TrainingRegistrationResponse {
  id: number;
  trainingSessionId: number;
  userId: number;
  userFullName: string;
  userEmail: string;
  status: TrainingRegistrationStatus;
  registeredAt: ISODateTime;
  cancelledAt: ISODateTime;
  completedAt: ISODateTime;
}

export interface TrainingSessionResponse {
  id: number;
  classId: number;
  className: string;
  classCode: string;
  title: string;
  description: string;
  trainerId: number;
  trainerFullName: string;
  trainerEmail: string;
  room: string;
  sessionDate: ISODate;
  startTime: ISODateTime;
  endTime: ISODateTime;
  sessionType: TrainingSessionType;
  meetingLink: string;
  capacity: number;
  enrolledCount: number;
  status: TrainingSessionStatus;
  createdAt: ISODateTime;
  updatedAt: ISODateTime;
}

export interface UpdateAttendanceRequest {
  records: AttendanceItemRequest[];
}

export interface UpdateClassAdminsRequest {
  userIds: number[];
}

export interface UpdateClassRequest {
  name?: string;
  location?: string;
  locationDetail?: string;
  fsu?: string;
  classTime?: string;
  startDate?: ISODate;
  endDate?: ISODate;
  duration?: string;
}

export interface UpdateClassStatusRequest {
  status: ClassStatus;
}

export interface UpdateClassTrainersRequest {
  trainers: ClassTrainerItemRequest[];
}

export interface UpdateMaterialFileRequest {
  fileName: string;
  fileUrl: string;
  fileSize?: number;
  contentType?: string;
}

export interface UpdatePermissionMatrixRequest {
  permissions: UpdatePermissionRequest[];
}

export interface UpdatePermissionRequest {
  roleId: number;
  resource: string;
  permissionLevel: PermissionLevel;
}

export interface UpdateQuestionRequest {
  content: string;
  questionType: QuestionType;
  category: string;
  difficulty: QuestionDifficulty;
  optionsJson: JsonNode;
  correctAnswersJson: JsonNode;
  explanation?: string;
}

export interface UpdateQuizQuestionsRequest {
  questions: QuizQuestionItemRequest[];
}

export interface UpdateQuizRequest {
  title: string;
  description?: string;
  durationMinutes: number;
  passingScore: number;
  maxAttempts: number;
  randomize?: boolean;
  category: string;
  openDate?: ISODate;
  closeDate?: ISODate;
}

export interface UpdateQuizStatusRequest {
  status: QuizStatus;
}

export interface UpdateSettingsRequest {
  settings: Record<string, Record<string, string>>;
}

export interface UpdateSyllabusOutputStandardsRequest {
  standards: string[];
}

export interface UpdateSyllabusRequest {
  name: string;
  version: string;
  levelName: string;
  attendees: number;
  duration?: string;
  technicalRequirements?: string;
  courseObjectives?: string;
  rules?: string;
  timeAllocAssignmentLab: number;
  timeAllocConceptLecture: number;
  timeAllocGuideReview: number;
  timeAllocTestQuiz: number;
  assessQuizPct: number;
  assessAssignmentPct: number;
  assessFinalPct: number;
  assessmentText?: string;
}

export interface UpdateSyllabusStatusRequest {
  status: SyllabusStatus;
}

export interface UpdateTrainingProgramRequest {
  name: string;
  duration?: string;
  totalHours?: number;
  version: string;
}

export interface UpdateTrainingProgramStatusRequest {
  status: TrainingProgramStatus;
}

export interface UpdateTrainingProgramSyllabusesRequest {
  syllabuses: TrainingProgramSyllabusItemRequest[];
}

export interface UpdateTrainingSessionRequest {
  title: string;
  description?: string;
  trainerId: number;
  room?: string;
  sessionDate: ISODate;
  startTime: ISODateTime;
  endTime: ISODateTime;
  sessionType: TrainingSessionType;
  meetingLink?: string;
  capacity: number;
}

export interface UpdateTrainingSessionStatusRequest {
  status: TrainingSessionStatus;
}

export interface UpdateUserRequest {
  fullName: string;
  email: string;
  phone?: string;
  dateOfBirth?: ISODate;
  gender: Gender;
  avatarUrl?: string;
  roleIds: number[];
}

export interface UpdateUserStatusRequest {
  status: UserStatus;
}

export interface UserResponse {
  id: number;
  fullName: string;
  email: string;
  phone: string;
  dateOfBirth: ISODate;
  gender: Gender;
  avatarUrl: string;
  status: UserStatus;
  roles: RoleResponse[];
  createdAt: ISODateTime;
  updatedAt: ISODateTime;
}

export const API_ENDPOINTS = [
  {
    key: "get.audit_logs",
    tag: "Audit Logs",
    summary: "List audit logs",
    method: "GET",
    path: "/api/v1/audit-logs",
    auth: true,
    query: [{"name":"userId","type":"number","required":false},{"name":"entityType","type":"string","required":false},{"name":"entityId","type":"number","required":false},{"name":"page","type":"number","required":false,"default":"1"},{"name":"limit","type":"number","required":false,"default":"20"}],
    response: "PageResponse<AuditLogResponse>",
  },
  {
    key: "patch.auth.change_password",
    tag: "Authentication",
    summary: "Change current user password",
    method: "PATCH",
    path: "/api/v1/auth/change-password",
    auth: true,
    request: "ChangePasswordRequest",
    response: "ApiResponse<null>",
  },
  {
    key: "post.auth.forgot_password",
    tag: "Authentication",
    summary: "Request password reset OTP",
    method: "POST",
    path: "/api/v1/auth/forgot-password",
    auth: false,
    request: "ForgotPasswordRequest",
    response: "ApiResponse<null>",
  },
  {
    key: "post.auth.google",
    tag: "Authentication",
    summary: "Authenticate with Google OAuth token",
    method: "POST",
    path: "/api/v1/auth/google",
    auth: false,
    request: "GoogleLoginRequest",
    response: "ApiResponse<AuthResponse>",
  },
  {
    key: "post.auth.login",
    tag: "Authentication",
    summary: "Authenticate with email and password",
    method: "POST",
    path: "/api/v1/auth/login",
    auth: false,
    request: "LoginRequest",
    response: "ApiResponse<AuthResponse>",
  },
  {
    key: "post.auth.logout",
    tag: "Authentication",
    summary: "Logout and revoke refresh token",
    method: "POST",
    path: "/api/v1/auth/logout",
    auth: false,
    request: "LogoutRequest",
    response: "ApiResponse<null>",
  },
  {
    key: "post.auth.refresh",
    tag: "Authentication",
    summary: "Refresh access token",
    method: "POST",
    path: "/api/v1/auth/refresh",
    auth: false,
    request: "RefreshTokenRequest",
    response: "ApiResponse<AuthResponse>",
  },
  {
    key: "post.auth.reset_password",
    tag: "Authentication",
    summary: "Reset password using OTP",
    method: "POST",
    path: "/api/v1/auth/reset-password",
    auth: false,
    request: "ResetPasswordRequest",
    response: "ApiResponse<null>",
  },
  {
    key: "get.classes.by_id.admins",
    tag: "Classes",
    summary: "List admins",
    method: "GET",
    path: "/api/v1/classes/{id}/admins",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    response: "ApiResponse<ClassAdminResponse[]>",
  },
  {
    key: "put.classes.by_id.admins",
    tag: "Classes",
    summary: "Replace admins",
    method: "PUT",
    path: "/api/v1/classes/{id}/admins",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    request: "UpdateClassAdminsRequest",
    response: "ApiResponse<ClassAdminResponse[]>",
  },
  {
    key: "patch.classes.by_id.status",
    tag: "Classes",
    summary: "Update status",
    method: "PATCH",
    path: "/api/v1/classes/{id}/status",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    request: "UpdateClassStatusRequest",
    response: "ApiResponse<ClassResponse>",
  },
  {
    key: "get.classes.by_id.trainers",
    tag: "Classes",
    summary: "List trainers",
    method: "GET",
    path: "/api/v1/classes/{id}/trainers",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    response: "ApiResponse<ClassTrainerResponse[]>",
  },
  {
    key: "put.classes.by_id.trainers",
    tag: "Classes",
    summary: "Replace trainers",
    method: "PUT",
    path: "/api/v1/classes/{id}/trainers",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    request: "UpdateClassTrainersRequest",
    response: "ApiResponse<ClassTrainerResponse[]>",
  },
  {
    key: "delete.classes.by_id",
    tag: "Classes",
    summary: "Delete classes",
    method: "DELETE",
    path: "/api/v1/classes/{id}",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    response: "void",
  },
  {
    key: "get.classes.by_id",
    tag: "Classes",
    summary: "Get classes detail",
    method: "GET",
    path: "/api/v1/classes/{id}",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    response: "ApiResponse<ClassResponse>",
  },
  {
    key: "put.classes.by_id",
    tag: "Classes",
    summary: "Update classes",
    method: "PUT",
    path: "/api/v1/classes/{id}",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    request: "UpdateClassRequest",
    response: "ApiResponse<ClassResponse>",
  },
  {
    key: "get.classes",
    tag: "Classes",
    summary: "List classes",
    method: "GET",
    path: "/api/v1/classes",
    auth: true,
    query: [{"name":"status","type":"ClassStatus","required":false},{"name":"trainingProgramId","type":"number","required":false},{"name":"keyword","type":"string","required":false},{"name":"page","type":"number","required":false,"default":"1"},{"name":"limit","type":"number","required":false,"default":"20"}],
    response: "PageResponse<ClassResponse>",
  },
  {
    key: "post.classes",
    tag: "Classes",
    summary: "Create classes",
    method: "POST",
    path: "/api/v1/classes",
    auth: true,
    request: "CreateClassRequest",
    response: "ApiResponse<ClassResponse>",
  },
  {
    key: "delete.materials.by_id",
    tag: "Materials",
    summary: "Delete materials",
    method: "DELETE",
    path: "/api/v1/materials/{id}",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    response: "void",
  },
  {
    key: "get.materials.by_id",
    tag: "Materials",
    summary: "Get materials detail",
    method: "GET",
    path: "/api/v1/materials/{id}",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    response: "ApiResponse<MaterialFileResponse>",
  },
  {
    key: "put.materials.by_id",
    tag: "Materials",
    summary: "Update materials",
    method: "PUT",
    path: "/api/v1/materials/{id}",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    request: "UpdateMaterialFileRequest",
    response: "ApiResponse<MaterialFileResponse>",
  },
  {
    key: "get.materials",
    tag: "Materials",
    summary: "List materials",
    method: "GET",
    path: "/api/v1/materials",
    auth: true,
    query: [{"name":"syllabusId","type":"number","required":false},{"name":"topicId","type":"number","required":false},{"name":"keyword","type":"string","required":false},{"name":"page","type":"number","required":false,"default":"1"},{"name":"limit","type":"number","required":false,"default":"20"}],
    response: "PageResponse<MaterialFileResponse>",
  },
  {
    key: "post.materials",
    tag: "Materials",
    summary: "Create materials",
    method: "POST",
    path: "/api/v1/materials",
    auth: true,
    request: "CreateMaterialRequest",
    response: "ApiResponse<MaterialFileResponse>",
  },
  {
    key: "get.me.materials",
    tag: "Materials",
    summary: "List current user assigned materials",
    method: "GET",
    path: "/api/v1/me/materials",
    auth: true,
    query: [{"name":"keyword","type":"string","required":false},{"name":"page","type":"number","required":false,"default":"1"},{"name":"limit","type":"number","required":false,"default":"20"}],
    response: "PageResponse<AssignedMaterialFileResponse>",
  },
  {
    key: "get.me.classes.by_classId.learning_content",
    tag: "My Learning",
    summary: "Learning Content",
    method: "GET",
    path: "/api/v1/me/classes/{classId}/learning-content",
    auth: true,
    pathParams: [{"name":"classId","type":"number"}],
    query: [{"name":"keyword","type":"string","required":false}],
    response: "ApiResponse<MyClassLearningContentResponse>",
  },
  {
    key: "get.me.classes.by_classId.progress",
    tag: "My Learning",
    summary: "Progress",
    method: "GET",
    path: "/api/v1/me/classes/{classId}/progress",
    auth: true,
    pathParams: [{"name":"classId","type":"number"}],
    response: "ApiResponse<MyClassProgressResponse>",
  },
  {
    key: "get.me.classes.by_classId",
    tag: "My Learning",
    summary: "Class Detail",
    method: "GET",
    path: "/api/v1/me/classes/{classId}",
    auth: true,
    pathParams: [{"name":"classId","type":"number"}],
    response: "ApiResponse<MyClassDetailResponse>",
  },
  {
    key: "get.me.classes",
    tag: "My Learning",
    summary: "Classes",
    method: "GET",
    path: "/api/v1/me/classes",
    auth: true,
    query: [{"name":"keyword","type":"string","required":false},{"name":"page","type":"number","required":false,"default":"1"},{"name":"limit","type":"number","required":false,"default":"20"}],
    response: "PageResponse<ClassResponse>",
  },
  {
    key: "get.me.attendance",
    tag: "My Training",
    summary: "Attendance",
    method: "GET",
    path: "/api/v1/me/attendance",
    auth: true,
    query: [{"name":"status","type":"AttendanceStatus","required":false},{"name":"fromDate","type":"ISODate","required":false},{"name":"toDate","type":"ISODate","required":false},{"name":"keyword","type":"string","required":false},{"name":"page","type":"number","required":false,"default":"1"},{"name":"limit","type":"number","required":false,"default":"20"}],
    response: "PageResponse<MyAttendanceResponse>",
  },
  {
    key: "get.me.class_admin_dashboard",
    tag: "My Training",
    summary: "Get current class admin dashboard",
    method: "GET",
    path: "/api/v1/me/class-admin-dashboard",
    auth: true,
    response: "ApiResponse<MyClassAdminDashboardResponse>",
  },
  {
    key: "get.me.trainer_dashboard",
    tag: "My Training",
    summary: "Get current trainer teaching dashboard",
    method: "GET",
    path: "/api/v1/me/trainer-dashboard",
    auth: true,
    response: "ApiResponse<MyTrainerDashboardResponse>",
  },
  {
    key: "get.me.training_dashboard",
    tag: "My Training",
    summary: "Get current trainee learning dashboard",
    method: "GET",
    path: "/api/v1/me/training-dashboard",
    auth: true,
    response: "ApiResponse<MyTrainingDashboardResponse>",
  },
  {
    key: "get.me.training_registrations",
    tag: "My Training",
    summary: "Registrations",
    method: "GET",
    path: "/api/v1/me/training-registrations",
    auth: true,
    query: [{"name":"registrationStatus","type":"TrainingRegistrationStatus","required":false},{"name":"sessionStatus","type":"TrainingSessionStatus","required":false},{"name":"fromDate","type":"ISODate","required":false},{"name":"toDate","type":"ISODate","required":false},{"name":"keyword","type":"string","required":false},{"name":"page","type":"number","required":false,"default":"1"},{"name":"limit","type":"number","required":false,"default":"20"}],
    response: "PageResponse<MyTrainingRegistrationResponse>",
  },
  {
    key: "get.me.training_sessions",
    tag: "My Training",
    summary: "Sessions",
    method: "GET",
    path: "/api/v1/me/training-sessions",
    auth: true,
    query: [{"name":"registrationStatus","type":"TrainingRegistrationStatus","required":false},{"name":"sessionStatus","type":"TrainingSessionStatus","required":false},{"name":"fromDate","type":"ISODate","required":false},{"name":"toDate","type":"ISODate","required":false},{"name":"keyword","type":"string","required":false},{"name":"page","type":"number","required":false,"default":"1"},{"name":"limit","type":"number","required":false,"default":"20"}],
    response: "PageResponse<MyTrainingSessionResponse>",
  },
  {
    key: "patch.notifications.by_id.read",
    tag: "Notifications",
    summary: "Mark notification as read",
    method: "PATCH",
    path: "/api/v1/notifications/{id}/read",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    response: "ApiResponse<NotificationResponse>",
  },
  {
    key: "get.notifications",
    tag: "Notifications",
    summary: "List notifications",
    method: "GET",
    path: "/api/v1/notifications",
    auth: true,
    query: [{"name":"page","type":"number","required":false,"default":"1"},{"name":"limit","type":"number","required":false,"default":"20"}],
    response: "PageResponse<NotificationResponse>",
  },
  {
    key: "delete.questions.by_id",
    tag: "Questions",
    summary: "Delete questions",
    method: "DELETE",
    path: "/api/v1/questions/{id}",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    response: "void",
  },
  {
    key: "get.questions.by_id",
    tag: "Questions",
    summary: "Get questions detail",
    method: "GET",
    path: "/api/v1/questions/{id}",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    response: "ApiResponse<QuestionResponse>",
  },
  {
    key: "put.questions.by_id",
    tag: "Questions",
    summary: "Update questions",
    method: "PUT",
    path: "/api/v1/questions/{id}",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    request: "UpdateQuestionRequest",
    response: "ApiResponse<QuestionResponse>",
  },
  {
    key: "get.questions",
    tag: "Questions",
    summary: "List questions",
    method: "GET",
    path: "/api/v1/questions",
    auth: true,
    query: [{"name":"questionType","type":"QuestionType","required":false},{"name":"difficulty","type":"QuestionDifficulty","required":false},{"name":"category","type":"string","required":false},{"name":"keyword","type":"string","required":false},{"name":"page","type":"number","required":false,"default":"1"},{"name":"limit","type":"number","required":false,"default":"20"}],
    response: "PageResponse<QuestionResponse>",
  },
  {
    key: "post.questions",
    tag: "Questions",
    summary: "Create questions",
    method: "POST",
    path: "/api/v1/questions",
    auth: true,
    request: "CreateQuestionRequest",
    response: "ApiResponse<QuestionResponse>",
  },
  {
    key: "put.quiz_attempts.by_attemptId.answers",
    tag: "Quiz Attempts",
    summary: "Save quiz attempt answers",
    method: "PUT",
    path: "/api/v1/quiz-attempts/{attemptId}/answers",
    auth: true,
    pathParams: [{"name":"attemptId","type":"number"}],
    request: "SaveQuizAnswersRequest",
    response: "ApiResponse<QuizAttemptResponse>",
  },
  {
    key: "get.quiz_attempts.by_attemptId.review",
    tag: "Quiz Attempts",
    summary: "Review submitted quiz attempt",
    method: "GET",
    path: "/api/v1/quiz-attempts/{attemptId}/review",
    auth: true,
    pathParams: [{"name":"attemptId","type":"number"}],
    response: "ApiResponse<QuizAttemptReviewResponse>",
  },
  {
    key: "post.quiz_attempts.by_attemptId.submit",
    tag: "Quiz Attempts",
    summary: "Submit quiz attempt",
    method: "POST",
    path: "/api/v1/quiz-attempts/{attemptId}/submit",
    auth: true,
    pathParams: [{"name":"attemptId","type":"number"}],
    response: "ApiResponse<QuizAttemptResponse>",
  },
  {
    key: "get.quiz_attempts.by_attemptId",
    tag: "Quiz Attempts",
    summary: "Get quiz attempts detail",
    method: "GET",
    path: "/api/v1/quiz-attempts/{attemptId}",
    auth: true,
    pathParams: [{"name":"attemptId","type":"number"}],
    response: "ApiResponse<QuizAttemptResponse>",
  },
  {
    key: "post.quizzes.by_quizId.attempts",
    tag: "Quiz Attempts",
    summary: "Start quiz attempt",
    method: "POST",
    path: "/api/v1/quizzes/{quizId}/attempts",
    auth: true,
    pathParams: [{"name":"quizId","type":"number"}],
    response: "ApiResponse<QuizAttemptResponse>",
  },
  {
    key: "get.quizzes.assigned",
    tag: "Quiz Attempts",
    summary: "List assigned quizzes for current trainee",
    method: "GET",
    path: "/api/v1/quizzes/assigned",
    auth: true,
    query: [{"name":"page","type":"number","required":false,"default":"1"},{"name":"limit","type":"number","required":false,"default":"20"}],
    response: "PageResponse<AssignedQuizResponse>",
  },
  {
    key: "get.quizzes.by_quizId.attempt_summary",
    tag: "Quiz Results",
    summary: "Get summary",
    method: "GET",
    path: "/api/v1/quizzes/{quizId}/attempt-summary",
    auth: true,
    pathParams: [{"name":"quizId","type":"number"}],
    query: [{"name":"classId","type":"number","required":false},{"name":"trainingSessionId","type":"number","required":false}],
    response: "ApiResponse<QuizAttemptSummaryResponse>",
  },
  {
    key: "get.quizzes.by_quizId.attempts.by_attemptId",
    tag: "Quiz Results",
    summary: "Get attempt detail detail",
    method: "GET",
    path: "/api/v1/quizzes/{quizId}/attempts/{attemptId}",
    auth: true,
    pathParams: [{"name":"quizId","type":"number"},{"name":"attemptId","type":"number"}],
    response: "ApiResponse<QuizAttemptReviewResponse>",
  },
  {
    key: "get.quizzes.by_quizId.attempts",
    tag: "Quiz Results",
    summary: "List attempts",
    method: "GET",
    path: "/api/v1/quizzes/{quizId}/attempts",
    auth: true,
    pathParams: [{"name":"quizId","type":"number"}],
    query: [{"name":"status","type":"QuizAttemptStatus","required":false},{"name":"passed","type":"boolean","required":false},{"name":"userId","type":"number","required":false},{"name":"classId","type":"number","required":false},{"name":"trainingSessionId","type":"number","required":false},{"name":"page","type":"number","required":false,"default":"1"},{"name":"limit","type":"number","required":false,"default":"20"}],
    response: "PageResponse<QuizAttemptResultResponse>",
  },
  {
    key: "delete.quizzes.by_id.assignments.by_assignmentId",
    tag: "Quizzes",
    summary: "Delete assignment",
    method: "DELETE",
    path: "/api/v1/quizzes/{id}/assignments/{assignmentId}",
    auth: true,
    pathParams: [{"name":"id","type":"number"},{"name":"assignmentId","type":"number"}],
    response: "void",
  },
  {
    key: "get.quizzes.by_id.assignments",
    tag: "Quizzes",
    summary: "List assignments",
    method: "GET",
    path: "/api/v1/quizzes/{id}/assignments",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    response: "ApiResponse<QuizAssignmentResponse[]>",
  },
  {
    key: "post.quizzes.by_id.assignments",
    tag: "Quizzes",
    summary: "Assign",
    method: "POST",
    path: "/api/v1/quizzes/{id}/assignments",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    request: "CreateQuizAssignmentRequest",
    response: "ApiResponse<QuizAssignmentResponse>",
  },
  {
    key: "get.quizzes.by_id.questions",
    tag: "Quizzes",
    summary: "List questions",
    method: "GET",
    path: "/api/v1/quizzes/{id}/questions",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    response: "ApiResponse<QuizQuestionResponse[]>",
  },
  {
    key: "put.quizzes.by_id.questions",
    tag: "Quizzes",
    summary: "Replace questions",
    method: "PUT",
    path: "/api/v1/quizzes/{id}/questions",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    request: "UpdateQuizQuestionsRequest",
    response: "ApiResponse<QuizQuestionResponse[]>",
  },
  {
    key: "patch.quizzes.by_id.status",
    tag: "Quizzes",
    summary: "Update status",
    method: "PATCH",
    path: "/api/v1/quizzes/{id}/status",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    request: "UpdateQuizStatusRequest",
    response: "ApiResponse<QuizResponse>",
  },
  {
    key: "delete.quizzes.by_id",
    tag: "Quizzes",
    summary: "Delete quizzes",
    method: "DELETE",
    path: "/api/v1/quizzes/{id}",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    response: "void",
  },
  {
    key: "get.quizzes.by_id",
    tag: "Quizzes",
    summary: "Get quizzes detail",
    method: "GET",
    path: "/api/v1/quizzes/{id}",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    response: "ApiResponse<QuizResponse>",
  },
  {
    key: "put.quizzes.by_id",
    tag: "Quizzes",
    summary: "Update quizzes",
    method: "PUT",
    path: "/api/v1/quizzes/{id}",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    request: "UpdateQuizRequest",
    response: "ApiResponse<QuizResponse>",
  },
  {
    key: "get.quizzes",
    tag: "Quizzes",
    summary: "List quizzes",
    method: "GET",
    path: "/api/v1/quizzes",
    auth: true,
    query: [{"name":"status","type":"QuizStatus","required":false},{"name":"category","type":"string","required":false},{"name":"keyword","type":"string","required":false},{"name":"page","type":"number","required":false,"default":"1"},{"name":"limit","type":"number","required":false,"default":"20"}],
    response: "PageResponse<QuizResponse>",
  },
  {
    key: "post.quizzes",
    tag: "Quizzes",
    summary: "Create quizzes",
    method: "POST",
    path: "/api/v1/quizzes",
    auth: true,
    request: "CreateQuizRequest",
    response: "ApiResponse<QuizResponse>",
  },
  {
    key: "get.roles.permissions",
    tag: "Roles and Permissions",
    summary: "Get role permission matrix",
    method: "GET",
    path: "/api/v1/roles/permissions",
    auth: true,
    response: "ApiResponse<PermissionResponse[]>",
  },
  {
    key: "put.roles.permissions",
    tag: "Roles and Permissions",
    summary: "Update role permission matrix",
    method: "PUT",
    path: "/api/v1/roles/permissions",
    auth: true,
    request: "UpdatePermissionMatrixRequest",
    response: "ApiResponse<PermissionResponse[]>",
  },
  {
    key: "get.roles",
    tag: "Roles and Permissions",
    summary: "List roles",
    method: "GET",
    path: "/api/v1/roles",
    auth: true,
    response: "ApiResponse<RoleResponse[]>",
  },
  {
    key: "get.settings",
    tag: "Settings",
    summary: "Get settings detail",
    method: "GET",
    path: "/api/v1/settings",
    auth: true,
    query: [{"name":"category","type":"string","required":false}],
    response: "ApiResponse<SettingsResponse>",
  },
  {
    key: "put.settings",
    tag: "Settings",
    summary: "Update settings",
    method: "PUT",
    path: "/api/v1/settings",
    auth: true,
    request: "UpdateSettingsRequest",
    response: "ApiResponse<SettingsResponse>",
  },
  {
    key: "post.syllabuses.by_id.days.by_dayId.units",
    tag: "Syllabus",
    summary: "Create unit",
    method: "POST",
    path: "/api/v1/syllabuses/{id}/days/{dayId}/units",
    auth: true,
    pathParams: [{"name":"id","type":"number"},{"name":"dayId","type":"number"}],
    request: "CreateSyllabusUnitRequest",
    response: "ApiResponse<SyllabusUnitResponse>",
  },
  {
    key: "delete.syllabuses.by_id.days.by_dayId",
    tag: "Syllabus",
    summary: "Delete day",
    method: "DELETE",
    path: "/api/v1/syllabuses/{id}/days/{dayId}",
    auth: true,
    pathParams: [{"name":"id","type":"number"},{"name":"dayId","type":"number"}],
    response: "void",
  },
  {
    key: "put.syllabuses.by_id.days.by_dayId",
    tag: "Syllabus",
    summary: "Update day",
    method: "PUT",
    path: "/api/v1/syllabuses/{id}/days/{dayId}",
    auth: true,
    pathParams: [{"name":"id","type":"number"},{"name":"dayId","type":"number"}],
    request: "CreateSyllabusDayRequest",
    response: "ApiResponse<SyllabusDayResponse>",
  },
  {
    key: "post.syllabuses.by_id.days",
    tag: "Syllabus",
    summary: "Create day",
    method: "POST",
    path: "/api/v1/syllabuses/{id}/days",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    request: "CreateSyllabusDayRequest",
    response: "ApiResponse<SyllabusDayResponse>",
  },
  {
    key: "get.syllabuses.by_id.outline",
    tag: "Syllabus",
    summary: "Get outline detail",
    method: "GET",
    path: "/api/v1/syllabuses/{id}/outline",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    response: "ApiResponse<SyllabusDayResponse[]>",
  },
  {
    key: "get.syllabuses.by_id.output_standards",
    tag: "Syllabus",
    summary: "Get output standards detail",
    method: "GET",
    path: "/api/v1/syllabuses/{id}/output-standards",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    response: "ApiResponse<string[]>",
  },
  {
    key: "put.syllabuses.by_id.output_standards",
    tag: "Syllabus",
    summary: "Replace output standards",
    method: "PUT",
    path: "/api/v1/syllabuses/{id}/output-standards",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    request: "UpdateSyllabusOutputStandardsRequest",
    response: "ApiResponse<string[]>",
  },
  {
    key: "patch.syllabuses.by_id.status",
    tag: "Syllabus",
    summary: "Update status",
    method: "PATCH",
    path: "/api/v1/syllabuses/{id}/status",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    request: "UpdateSyllabusStatusRequest",
    response: "ApiResponse<SyllabusResponse>",
  },
  {
    key: "delete.syllabuses.by_id.topics.by_topicId.materials.by_materialId",
    tag: "Syllabus",
    summary: "Delete material",
    method: "DELETE",
    path: "/api/v1/syllabuses/{id}/topics/{topicId}/materials/{materialId}",
    auth: true,
    pathParams: [{"name":"id","type":"number"},{"name":"topicId","type":"number"},{"name":"materialId","type":"number"}],
    response: "void",
  },
  {
    key: "get.syllabuses.by_id.topics.by_topicId.materials",
    tag: "Syllabus",
    summary: "List materials",
    method: "GET",
    path: "/api/v1/syllabuses/{id}/topics/{topicId}/materials",
    auth: true,
    pathParams: [{"name":"id","type":"number"},{"name":"topicId","type":"number"}],
    response: "ApiResponse<MaterialFileResponse[]>",
  },
  {
    key: "post.syllabuses.by_id.topics.by_topicId.materials",
    tag: "Syllabus",
    summary: "Create material",
    method: "POST",
    path: "/api/v1/syllabuses/{id}/topics/{topicId}/materials",
    auth: true,
    pathParams: [{"name":"id","type":"number"},{"name":"topicId","type":"number"}],
    request: "CreateMaterialFileRequest",
    response: "ApiResponse<MaterialFileResponse>",
  },
  {
    key: "delete.syllabuses.by_id.topics.by_topicId",
    tag: "Syllabus",
    summary: "Delete topic",
    method: "DELETE",
    path: "/api/v1/syllabuses/{id}/topics/{topicId}",
    auth: true,
    pathParams: [{"name":"id","type":"number"},{"name":"topicId","type":"number"}],
    response: "void",
  },
  {
    key: "put.syllabuses.by_id.topics.by_topicId",
    tag: "Syllabus",
    summary: "Update topic",
    method: "PUT",
    path: "/api/v1/syllabuses/{id}/topics/{topicId}",
    auth: true,
    pathParams: [{"name":"id","type":"number"},{"name":"topicId","type":"number"}],
    request: "CreateSyllabusTopicRequest",
    response: "ApiResponse<SyllabusTopicResponse>",
  },
  {
    key: "post.syllabuses.by_id.units.by_unitId.topics",
    tag: "Syllabus",
    summary: "Create topic",
    method: "POST",
    path: "/api/v1/syllabuses/{id}/units/{unitId}/topics",
    auth: true,
    pathParams: [{"name":"id","type":"number"},{"name":"unitId","type":"number"}],
    request: "CreateSyllabusTopicRequest",
    response: "ApiResponse<SyllabusTopicResponse>",
  },
  {
    key: "delete.syllabuses.by_id.units.by_unitId",
    tag: "Syllabus",
    summary: "Delete unit",
    method: "DELETE",
    path: "/api/v1/syllabuses/{id}/units/{unitId}",
    auth: true,
    pathParams: [{"name":"id","type":"number"},{"name":"unitId","type":"number"}],
    response: "void",
  },
  {
    key: "put.syllabuses.by_id.units.by_unitId",
    tag: "Syllabus",
    summary: "Update unit",
    method: "PUT",
    path: "/api/v1/syllabuses/{id}/units/{unitId}",
    auth: true,
    pathParams: [{"name":"id","type":"number"},{"name":"unitId","type":"number"}],
    request: "CreateSyllabusUnitRequest",
    response: "ApiResponse<SyllabusUnitResponse>",
  },
  {
    key: "delete.syllabuses.by_id",
    tag: "Syllabus",
    summary: "Delete syllabus",
    method: "DELETE",
    path: "/api/v1/syllabuses/{id}",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    response: "void",
  },
  {
    key: "get.syllabuses.by_id",
    tag: "Syllabus",
    summary: "Get syllabus detail",
    method: "GET",
    path: "/api/v1/syllabuses/{id}",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    response: "ApiResponse<SyllabusResponse>",
  },
  {
    key: "put.syllabuses.by_id",
    tag: "Syllabus",
    summary: "Update syllabus",
    method: "PUT",
    path: "/api/v1/syllabuses/{id}",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    request: "UpdateSyllabusRequest",
    response: "ApiResponse<SyllabusResponse>",
  },
  {
    key: "post.syllabuses.import",
    tag: "Syllabus",
    summary: "Import syllabuses from CSV file",
    method: "POST",
    path: "/api/v1/syllabuses/import",
    auth: true,
    request: "FormData",
    formData: [{"name":"file","type":"File","required":true}],
    response: "ApiResponse<SyllabusImportResponse>",
  },
  {
    key: "get.syllabuses",
    tag: "Syllabus",
    summary: "List syllabus",
    method: "GET",
    path: "/api/v1/syllabuses",
    auth: true,
    query: [{"name":"status","type":"SyllabusStatus","required":false},{"name":"levelName","type":"string","required":false},{"name":"keyword","type":"string","required":false},{"name":"page","type":"number","required":false,"default":"1"},{"name":"limit","type":"number","required":false,"default":"20"}],
    response: "PageResponse<SyllabusResponse>",
  },
  {
    key: "post.syllabuses",
    tag: "Syllabus",
    summary: "Create syllabus",
    method: "POST",
    path: "/api/v1/syllabuses",
    auth: true,
    request: "CreateSyllabusRequest",
    response: "ApiResponse<SyllabusResponse>",
  },
  {
    key: "get.me.feedback",
    tag: "Training Feedback",
    summary: "List current user training feedback",
    method: "GET",
    path: "/api/v1/me/feedback",
    auth: true,
    query: [{"name":"page","type":"number","required":false,"default":"1"},{"name":"limit","type":"number","required":false,"default":"20"}],
    response: "PageResponse<TrainingFeedbackResponse>",
  },
  {
    key: "get.training_sessions.by_id.feedback_summary",
    tag: "Training Feedback",
    summary: "Get summary",
    method: "GET",
    path: "/api/v1/training-sessions/{id}/feedback-summary",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    response: "ApiResponse<TrainingFeedbackSummaryResponse>",
  },
  {
    key: "post.training_sessions.by_id.feedback",
    tag: "Training Feedback",
    summary: "Submit quiz attempt",
    method: "POST",
    path: "/api/v1/training-sessions/{id}/feedback",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    request: "CreateTrainingFeedbackRequest",
    response: "ApiResponse<TrainingFeedbackResponse>",
  },
  {
    key: "patch.training_programs.by_id.status",
    tag: "Training Programs",
    summary: "Update status",
    method: "PATCH",
    path: "/api/v1/training-programs/{id}/status",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    request: "UpdateTrainingProgramStatusRequest",
    response: "ApiResponse<TrainingProgramResponse>",
  },
  {
    key: "get.training_programs.by_id.syllabuses",
    tag: "Training Programs",
    summary: "List syllabuses",
    method: "GET",
    path: "/api/v1/training-programs/{id}/syllabuses",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    response: "ApiResponse<TrainingProgramSyllabusResponse[]>",
  },
  {
    key: "put.training_programs.by_id.syllabuses",
    tag: "Training Programs",
    summary: "Replace syllabuses",
    method: "PUT",
    path: "/api/v1/training-programs/{id}/syllabuses",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    request: "UpdateTrainingProgramSyllabusesRequest",
    response: "ApiResponse<TrainingProgramSyllabusResponse[]>",
  },
  {
    key: "delete.training_programs.by_id",
    tag: "Training Programs",
    summary: "Delete training programs",
    method: "DELETE",
    path: "/api/v1/training-programs/{id}",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    response: "void",
  },
  {
    key: "get.training_programs.by_id",
    tag: "Training Programs",
    summary: "Get training programs detail",
    method: "GET",
    path: "/api/v1/training-programs/{id}",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    response: "ApiResponse<TrainingProgramResponse>",
  },
  {
    key: "put.training_programs.by_id",
    tag: "Training Programs",
    summary: "Update training programs",
    method: "PUT",
    path: "/api/v1/training-programs/{id}",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    request: "UpdateTrainingProgramRequest",
    response: "ApiResponse<TrainingProgramResponse>",
  },
  {
    key: "get.training_programs",
    tag: "Training Programs",
    summary: "List training programs",
    method: "GET",
    path: "/api/v1/training-programs",
    auth: true,
    query: [{"name":"status","type":"TrainingProgramStatus","required":false},{"name":"keyword","type":"string","required":false},{"name":"page","type":"number","required":false,"default":"1"},{"name":"limit","type":"number","required":false,"default":"20"}],
    response: "PageResponse<TrainingProgramResponse>",
  },
  {
    key: "post.training_programs",
    tag: "Training Programs",
    summary: "Create training programs",
    method: "POST",
    path: "/api/v1/training-programs",
    auth: true,
    request: "CreateTrainingProgramRequest",
    response: "ApiResponse<TrainingProgramResponse>",
  },
  {
    key: "get.training_sessions.by_id.attendance",
    tag: "Training Sessions",
    summary: "Attendance",
    method: "GET",
    path: "/api/v1/training-sessions/{id}/attendance",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    response: "ApiResponse<AttendanceRecordResponse[]>",
  },
  {
    key: "put.training_sessions.by_id.attendance",
    tag: "Training Sessions",
    summary: "Upsert Attendance",
    method: "PUT",
    path: "/api/v1/training-sessions/{id}/attendance",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    request: "UpdateAttendanceRequest",
    response: "ApiResponse<AttendanceRecordResponse[]>",
  },
  {
    key: "get.training_sessions.by_id.participants",
    tag: "Training Sessions",
    summary: "Participants",
    method: "GET",
    path: "/api/v1/training-sessions/{id}/participants",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    response: "ApiResponse<TrainingParticipantsResponse>",
  },
  {
    key: "delete.training_sessions.by_id.registrations.me",
    tag: "Training Sessions",
    summary: "Cancel My Registration",
    method: "DELETE",
    path: "/api/v1/training-sessions/{id}/registrations/me",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    response: "ApiResponse<TrainingRegistrationResponse>",
  },
  {
    key: "post.training_sessions.by_id.registrations",
    tag: "Training Sessions",
    summary: "Register",
    method: "POST",
    path: "/api/v1/training-sessions/{id}/registrations",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    response: "ApiResponse<TrainingRegistrationResponse>",
  },
  {
    key: "patch.training_sessions.by_id.status",
    tag: "Training Sessions",
    summary: "Update status",
    method: "PATCH",
    path: "/api/v1/training-sessions/{id}/status",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    request: "UpdateTrainingSessionStatusRequest",
    response: "ApiResponse<TrainingSessionResponse>",
  },
  {
    key: "get.training_sessions.by_id",
    tag: "Training Sessions",
    summary: "Get training sessions detail",
    method: "GET",
    path: "/api/v1/training-sessions/{id}",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    response: "ApiResponse<TrainingSessionResponse>",
  },
  {
    key: "put.training_sessions.by_id",
    tag: "Training Sessions",
    summary: "Update training sessions",
    method: "PUT",
    path: "/api/v1/training-sessions/{id}",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    request: "UpdateTrainingSessionRequest",
    response: "ApiResponse<TrainingSessionResponse>",
  },
  {
    key: "get.training_sessions",
    tag: "Training Sessions",
    summary: "List training sessions",
    method: "GET",
    path: "/api/v1/training-sessions",
    auth: true,
    query: [{"name":"status","type":"TrainingSessionStatus","required":false},{"name":"classId","type":"number","required":false},{"name":"trainerId","type":"number","required":false},{"name":"fromDate","type":"ISODate","required":false},{"name":"toDate","type":"ISODate","required":false},{"name":"keyword","type":"string","required":false},{"name":"page","type":"number","required":false,"default":"1"},{"name":"limit","type":"number","required":false,"default":"20"}],
    response: "PageResponse<TrainingSessionResponse>",
  },
  {
    key: "post.training_sessions",
    tag: "Training Sessions",
    summary: "Create training sessions",
    method: "POST",
    path: "/api/v1/training-sessions",
    auth: true,
    request: "CreateTrainingSessionRequest",
    response: "ApiResponse<TrainingSessionResponse>",
  },
  {
    key: "patch.users.by_id.status",
    tag: "Users",
    summary: "Update status",
    method: "PATCH",
    path: "/api/v1/users/{id}/status",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    request: "UpdateUserStatusRequest",
    response: "ApiResponse<UserResponse>",
  },
  {
    key: "get.users.by_id",
    tag: "Users",
    summary: "Get users detail",
    method: "GET",
    path: "/api/v1/users/{id}",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    response: "ApiResponse<UserResponse>",
  },
  {
    key: "put.users.by_id",
    tag: "Users",
    summary: "Update users",
    method: "PUT",
    path: "/api/v1/users/{id}",
    auth: true,
    pathParams: [{"name":"id","type":"number"}],
    request: "UpdateUserRequest",
    response: "ApiResponse<UserResponse>",
  },
  {
    key: "get.users",
    tag: "Users",
    summary: "List users",
    method: "GET",
    path: "/api/v1/users",
    auth: true,
    query: [{"name":"status","type":"UserStatus","required":false},{"name":"keyword","type":"string","required":false},{"name":"email","type":"string","required":false},{"name":"fullName","type":"string","required":false},{"name":"roleId","type":"number","required":false},{"name":"roleName","type":"string","required":false},{"name":"page","type":"number","required":false,"default":"1"},{"name":"limit","type":"number","required":false,"default":"20"}],
    response: "PageResponse<UserResponse>",
  },
  {
    key: "post.users",
    tag: "Users",
    summary: "Create users",
    method: "POST",
    path: "/api/v1/users",
    auth: true,
    request: "CreateUserRequest",
    response: "ApiResponse<UserResponse>",
  },
] as const satisfies readonly ApiEndpoint[];

export const apiEndpointByKey = Object.fromEntries(API_ENDPOINTS.map((endpoint) => [endpoint.key, endpoint]));

// Standard request headers for authenticated calls:
// Authorization: Bearer <accessToken>
// Accept-Language: en | vi
