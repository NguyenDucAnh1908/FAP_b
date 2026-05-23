package com.fap.common.util;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FileValidator {

	public boolean isPresent(MultipartFile file) {
		return file != null && !file.isEmpty();
	}
}
