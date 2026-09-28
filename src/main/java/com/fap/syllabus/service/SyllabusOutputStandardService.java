package com.fap.syllabus.service;

import com.fap.common.audit.AuditLogService;
import com.fap.common.exception.NotFoundException;
import com.fap.syllabus.dto.UpdateSyllabusOutputStandardsRequest;
import com.fap.syllabus.entity.Syllabus;
import com.fap.syllabus.entity.SyllabusOutputStandard;
import com.fap.syllabus.repository.SyllabusOutputStandardRepository;
import com.fap.syllabus.repository.SyllabusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class SyllabusOutputStandardService {

	private final SyllabusRepository syllabusRepository;
	private final SyllabusOutputStandardRepository outputStandardRepository;
	private final AuditLogService auditLogService;

	public SyllabusOutputStandardService(
			SyllabusRepository syllabusRepository,
			SyllabusOutputStandardRepository outputStandardRepository,
			AuditLogService auditLogService) {
		this.syllabusRepository = syllabusRepository;
		this.outputStandardRepository = outputStandardRepository;
		this.auditLogService = auditLogService;
	}

	@Transactional(readOnly = true)
	public List<String> list(Long syllabusId) {
		ensureSyllabusExists(syllabusId);
		return outputStandardRepository.findByIdSyllabusIdOrderByIdStandardCodeAsc(syllabusId).stream()
				.map(SyllabusOutputStandard::getStandardCode)
				.toList();
	}

	@Transactional
	public List<String> replace(Long syllabusId, UpdateSyllabusOutputStandardsRequest request) {
		Syllabus syllabus = findEditableSyllabus(syllabusId);
		outputStandardRepository.deleteByIdSyllabusId(syllabusId);
		List<SyllabusOutputStandard> outputStandards = request.standards().stream()
				.sorted(Comparator.naturalOrder())
				.map(standardCode -> SyllabusRules.createOutputStandard(syllabus, standardCode))
				.toList();
		outputStandardRepository.saveAll(outputStandards);
		auditLogService.record("UPDATE_SYLLABUS_OUTPUT_STANDARDS", "syllabus", syllabusId);
		return outputStandards.stream()
				.map(SyllabusOutputStandard::getStandardCode)
				.toList();
	}

	private void ensureSyllabusExists(Long syllabusId) {
		if (!syllabusRepository.existsById(syllabusId)) {
			throw new NotFoundException("syllabus", "Syllabus not found");
		}
	}

	private Syllabus findEditableSyllabus(Long syllabusId) {
		Syllabus syllabus = syllabusRepository.getOrThrow(syllabusId);
		SyllabusRules.ensureEditable(syllabus);
		return syllabus;
	}
}
