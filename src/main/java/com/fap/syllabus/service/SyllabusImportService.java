package com.fap.syllabus.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.BadRequestException;
import com.fap.syllabus.dto.SyllabusImportResponse;
import com.fap.syllabus.dto.SyllabusImportResponse.ImportError;
import com.fap.syllabus.dto.SyllabusResponse;
import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.enums.SyllabusStatus;
import com.fap.syllabus.mapper.SyllabusMapper;
import com.fap.syllabus.repository.SyllabusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SyllabusImportService {

	private static final String[] EXPECTED_HEADERS = {
			"name", "code", "version", "level_name", "attendees", "duration",
			"technical_requirements", "course_objectives", "rules",
			"time_alloc_assignment_lab", "time_alloc_concept_lecture",
			"time_alloc_guide_review", "time_alloc_test_quiz",
			"assess_quiz_pct", "assess_assignment_pct", "assess_final_pct",
			"assessment_text"
	};

	private final SyllabusRepository syllabusRepository;
	private final SyllabusMapper syllabusMapper;
	private final AuditLogService auditLogService;

	public SyllabusImportService(
			SyllabusRepository syllabusRepository,
			SyllabusMapper syllabusMapper,
			AuditLogService auditLogService) {
		this.syllabusRepository = syllabusRepository;
		this.syllabusMapper = syllabusMapper;
		this.auditLogService = auditLogService;
	}

	@Transactional
	public SyllabusImportResponse importCsv(MultipartFile file, Long currentUserId) {
		if (file.isEmpty()) {
			throw new BadRequestException("IMPORT_FILE_EMPTY", "Import file is empty");
		}
		String filename = file.getOriginalFilename();
		if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
			throw new BadRequestException("IMPORT_FILE_INVALID", "File must be a CSV file");
		}

		List<String[]> rows = parseCsv(file);
		if (rows.isEmpty()) {
			throw new BadRequestException("IMPORT_FILE_EMPTY", "CSV file has no data rows");
		}

		String[] headers = rows.get(0);
		validateHeaders(headers);

		List<ImportError> errors = new ArrayList<>();
		List<SyllabusResponse> createdSyllabuses = new ArrayList<>();
		Set<String> processedCodes = new HashSet<>();
		LocalDateTime now = LocalDateTime.now();

		for (int i = 1; i < rows.size(); i++) {
			int rowNum = i + 1;
			String[] row = rows.get(i);

			try {
				Syllabus syllabus = parseRow(row, headers, rowNum, errors, processedCodes, now, currentUserId);
				if (syllabus != null) {
					Syllabus saved = syllabusRepository.save(syllabus);
					processedCodes.add(saved.getCode().toUpperCase());
					createdSyllabuses.add(syllabusMapper.toResponse(saved));
					auditLogService.record("IMPORT_SYLLABUS", "syllabus", saved.getId());
				}
			} catch (Exception e) {
				errors.add(new ImportError(rowNum, "row", "Unexpected error: " + e.getMessage()));
			}
		}

		return new SyllabusImportResponse(
				rows.size() - 1,
				createdSyllabuses.size(),
				(rows.size() - 1) - createdSyllabuses.size(),
				errors.isEmpty() ? null : errors,
				createdSyllabuses
		);
	}

	private List<String[]> parseCsv(MultipartFile file) {
		List<String[]> rows = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.isBlank()) {
					rows.add(parseCsvLine(line));
				}
			}
		} catch (Exception e) {
			throw new BadRequestException("IMPORT_FILE_PARSE_ERROR", "Failed to parse CSV file: " + e.getMessage());
		}
		return rows;
	}

	private String[] parseCsvLine(String line) {
		List<String> values = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean inQuotes = false;

		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (c == '"') {
				if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
					current.append('"');
					i++;
				} else {
					inQuotes = !inQuotes;
				}
			} else if (c == ',' && !inQuotes) {
				values.add(current.toString().trim());
				current = new StringBuilder();
			} else {
				current.append(c);
			}
		}
		values.add(current.toString().trim());
		return values.toArray(new String[0]);
	}

	private void validateHeaders(String[] headers) {
		Set<String> headerSet = new HashSet<>();
		for (String header : headers) {
			headerSet.add(header.trim().toLowerCase());
		}
		for (String expected : EXPECTED_HEADERS) {
			if (!headerSet.contains(expected)) {
				throw new BadRequestException("IMPORT_HEADER_MISSING", "Missing required header: " + expected);
			}
		}
	}

	private Syllabus parseRow(
			String[] row,
			String[] headers,
			int rowNum,
			List<ImportError> errors,
			Set<String> processedCodes,
			LocalDateTime now,
		Long currentUserId) {

		if (row.length < headers.length) {
			row = Arrays.copyOf(row, headers.length);
		}

		String name = getValue(row, headers, "name");
		String code = getValue(row, headers, "code");

		if (name == null || name.isBlank()) {
			errors.add(new ImportError(rowNum, "name", "Name is required"));
			return null;
		}
		if (code == null || code.isBlank()) {
			errors.add(new ImportError(rowNum, "code", "Code is required"));
			return null;
		}

		String normalizedCode = code.trim().toUpperCase();
		if (processedCodes.contains(normalizedCode)) {
			errors.add(new ImportError(rowNum, "code", "Duplicate code in import file: " + code));
			return null;
		}
		if (syllabusRepository.existsByCodeIgnoreCase(normalizedCode)) {
			errors.add(new ImportError(rowNum, "code", "Code already exists in database: " + code));
			return null;
		}

		Integer assignmentLab = parseInteger(row, headers, "time_alloc_assignment_lab", 25);
		Integer conceptLecture = parseInteger(row, headers, "time_alloc_concept_lecture", 25);
		Integer guideReview = parseInteger(row, headers, "time_alloc_guide_review", 25);
		Integer testQuiz = parseInteger(row, headers, "time_alloc_test_quiz", 25);
		Integer quizPct = parseInteger(row, headers, "assess_quiz_pct", 30);
		Integer assignmentPct = parseInteger(row, headers, "assess_assignment_pct", 30);
		Integer finalPct = parseInteger(row, headers, "assess_final_pct", 40);

		if (assignmentLab + conceptLecture + guideReview + testQuiz != 100) {
			errors.add(new ImportError(rowNum, "time_allocation", "Time allocation total must be 100"));
			return null;
		}
		if (quizPct + assignmentPct + finalPct != 100) {
			errors.add(new ImportError(rowNum, "assessment", "Assessment total must be 100"));
			return null;
		}

		Syllabus syllabus = new Syllabus();
		syllabus.setName(name.trim());
		syllabus.setCode(normalizedCode);
		syllabus.setVersion(getValueOrDefault(row, headers, "version", "v1.0"));
		syllabus.setLevelName(getValueOrDefault(row, headers, "level_name", "All levels"));
		syllabus.setAttendees(parseInteger(row, headers, "attendees", 30));
		syllabus.setDuration(getValueOrDefault(row, headers, "duration", "1 day"));
		syllabus.setTechnicalRequirements(getValue(row, headers, "technical_requirements"));
		syllabus.setCourseObjectives(getValue(row, headers, "course_objectives"));
		syllabus.setRules(getValue(row, headers, "rules"));
		syllabus.setTimeAllocAssignmentLab(assignmentLab);
		syllabus.setTimeAllocConceptLecture(conceptLecture);
		syllabus.setTimeAllocGuideReview(guideReview);
		syllabus.setTimeAllocTestQuiz(testQuiz);
		syllabus.setAssessQuizPct(quizPct);
		syllabus.setAssessAssignmentPct(assignmentPct);
		syllabus.setAssessFinalPct(finalPct);
		syllabus.setAssessmentText(getValue(row, headers, "assessment_text"));
		syllabus.setStatus(SyllabusStatus.Drafting);
		syllabus.setCreatedAt(now);
		syllabus.setUpdatedAt(now);
		syllabus.setCreatedBy(currentUserId);
		syllabus.setUpdatedBy(currentUserId);

		return syllabus;
	}

	private String getValue(String[] row, String[] headers, String headerName) {
		for (int i = 0; i < headers.length; i++) {
			if (headers[i].trim().equalsIgnoreCase(headerName) && i < row.length) {
				return row[i].isBlank() ? null : row[i].trim();
			}
		}
		return null;
	}

	private String getValueOrDefault(String[] row, String[] headers, String headerName, String defaultValue) {
		String value = getValue(row, headers, headerName);
		return value == null || value.isBlank() ? defaultValue : value;
	}

	private Integer parseInteger(String[] row, String[] headers, String headerName, int defaultValue) {
		String value = getValue(row, headers, headerName);
		if (value == null || value.isBlank()) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
}

