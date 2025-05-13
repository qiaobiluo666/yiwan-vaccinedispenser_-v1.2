package com.yiwan.vaccinedispenser.core.pojo;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PageData<T> {
    private List<T> content;
    private int page;
    private int size;
    private long pages;
    private long total;
    private boolean first;
    private boolean last;

    public PageData(List<T> content, int page, int size, long total) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.total = total;
        long pages = total / (long)size;
        if (total % (long)size > 0L) {
            ++pages;
        }

        this.pages = pages;
        this.first = page <= 1;
        this.last = this.pages <= (long)page;
    }

    public <S> PageData<S> map(Function<T, S> fun) {
        List<S> otherContent = null;
        if (this.content != null) {
            otherContent = (List)this.content.stream().map(fun).collect(Collectors.toList());
        }

        return new PageData(otherContent, this.page, this.size, this.total);
    }

    public List<T> getContent() {
        return this.content;
    }

    public int getPage() {
        return this.page;
    }

    public int getSize() {
        return this.size;
    }

    public long getPages() {
        return this.pages;
    }

    public long getTotal() {
        return this.total;
    }

    public boolean isFirst() {
        return this.first;
    }

    public boolean isLast() {
        return this.last;
    }

    @Override
    public String toString() {
        return "PageData{" +
                "content=" + content +
                ", page=" + page +
                ", size=" + size +
                ", pages=" + pages +
                ", total=" + total +
                ", first=" + first +
                ", last=" + last +
                '}';
    }
}
