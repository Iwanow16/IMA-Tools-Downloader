package com.iwanow16.backend.extractor;

import com.iwanow16.backend.model.dto.VideoInfoDto;

public interface VideoExtractor {
    VideoInfoDto extractInfo(String url) throws Exception;
    String getServiceName();
}
