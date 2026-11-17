package io.github.mighil31.registrar.web;

import java.util.List;

/** One page of a list, and enough to draw the pager. */
public record PageResponse<T>(List<T> items, int page, int size, long total) {}
