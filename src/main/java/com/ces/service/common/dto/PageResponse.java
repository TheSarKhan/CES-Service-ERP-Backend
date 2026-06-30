package com.ces.service.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Lightweight, serializable view over a Spring Data {@link Page} that carries the
 * content together with {@link PageMeta}. Note pages are exposed to clients as
 * 1-based (SRS §6.3) while Spring Data is 0-based internally.
 *
 * @param <T> element type
 */
public record PageResponse<T>(
        List<T> content,
        PageMeta meta
) {

    public static <T> PageResponse<T> of(Page<T> page) {
        PageMeta meta = new PageMeta(
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalPages(),
                page.getTotalElements()
        );
        return new PageResponse<>(page.getContent(), meta);
    }

    public static <S, T> PageResponse<T> of(Page<S> page, Function<S, T> mapper) {
        PageMeta meta = new PageMeta(
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalPages(),
                page.getTotalElements()
        );
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(), meta);
    }
}
