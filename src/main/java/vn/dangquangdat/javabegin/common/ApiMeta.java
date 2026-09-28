package vn.dangquangdat.javabegin.common;

import org.springframework.data.domain.Page;

public record ApiMeta(
        long totalItems,
        int itemsPerPage,
        int totalPages,
        int currentPage,
        boolean hasNextPage,
        boolean hasPreviousPage
) {
    public static ApiMeta empty() {
        return new ApiMeta(0, 0, 0, 1, false, false);
    }

    public static ApiMeta from(Page<?> page) {
        return new ApiMeta(
                page.getTotalElements(),
                page.getSize(),
                page.getTotalPages(),
                page.getNumber() + 1,
                page.hasNext(),
                page.hasPrevious()
        );
    }
}

