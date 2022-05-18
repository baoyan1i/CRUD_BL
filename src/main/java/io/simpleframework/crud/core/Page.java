package io.simpleframework.crud.core;

import io.simpleframework.crud.mapper.mybatis.Pages;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;


@Data
@NoArgsConstructor
public class Page<T> implements Serializable {
    private static final long serialVersionUID = -1L;

    
    private List<T> items;
    
    private int pageNum;
    
    private int pageSize;
    
    private long total;
    
    private int pages;
    
    private long offset;

    private Page(Page<?> other) {
        this.items = new ArrayList<>();
        this.pageNum = other.getPageNum();
        this.pageSize = other.getPageSize();
        this.total = other.getTotal();
        this.pages = other.getPages();
        this.offset = other.getOffset();
    }

    public static <R> Page<R> of(int pageNum, int pageSize, long total) {
        Page<R> result = new Page<>();
        result.setItems(new ArrayList<>());
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setTotal(total);
        result.setPages(Pages.calcPages(total, pageSize));
        result.setOffset(Pages.calcStartRow(pageNum, pageSize));
        return result;
    }

    public <R> Page<R> convert(Function<? super T, ? extends R> mapper) {
        Page<R> result = new Page<>(this);
        List<R> collect = this.getItems().stream().map(mapper).collect(toList());
        result.setItems(collect);
        return result;
    }

}
