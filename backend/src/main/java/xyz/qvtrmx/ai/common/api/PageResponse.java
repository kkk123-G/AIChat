package xyz.qvtrmx.ai.common.api;

import java.util.List;

public record PageResponse<T>(List<T> records, long total, long current, long size, long pages) {
}
