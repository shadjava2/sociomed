package cd.senat.medical.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PageResponse<T> {

    private List<T> content;
    private int totalPages;
    private long totalElements;
    private int currentPage;
    private int size;
    private boolean first;
    private boolean last;
    private boolean empty;

    // Constructeurs
    public PageResponse() {}

    public PageResponse(List<T> content, int totalPages, long totalElements,
                        int currentPage, int size, boolean first, boolean last, boolean empty) {
        this.content = content;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.currentPage = currentPage;
        this.size = size;
        this.first = first;
        this.last = last;
        this.empty = empty;
    }

    /* ---------- Usines pratiques ---------- */

    /** Construit depuis un Page<T> Spring Data */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty()
        );
    }

    /** Construction manuelle si besoin */
    public static <T> PageResponse<T> of(List<T> content, int currentPage, int size,
                                         long totalElements, int totalPages,
                                         boolean first, boolean last, boolean empty) {
        return new PageResponse<>(content, totalPages, totalElements, currentPage, size, first, last, empty);
    }

    /**
     * Transforme le contenu vers un autre type (ex: Entity -> DTO) tout en gardant la pagination.
     * Utile si tu fais from(page).map(dtoMapper).
     */
    public <U> PageResponse<U> map(Function<? super T, ? extends U> mapper) {
        List<U> mapped = this.content == null ? List.of()
                : this.content.stream().map(mapper).collect(Collectors.toList());
        return new PageResponse<>(
                mapped,
                this.totalPages,
                this.totalElements,
                this.currentPage,
                this.size,
                this.first,
                this.last,
                this.empty
        );
    }

    // Getters / Setters
    public List<T> getContent() { return content; }
    public void setContent(List<T> content) { this.content = content; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public boolean isFirst() { return first; }
    public void setFirst(boolean first) { this.first = first; }

    public boolean isLast() { return last; }
    public void setLast(boolean last) { this.last = last; }

    public boolean isEmpty() { return empty; }
    public void setEmpty(boolean empty) { this.empty = empty; }
}
