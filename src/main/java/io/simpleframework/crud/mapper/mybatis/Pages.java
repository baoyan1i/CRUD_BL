package io.simpleframework.crud.mapper.mybatis;

import com.github.pagehelper.PageHelper;
import io.simpleframework.crud.core.Page;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author loyayz (loyayz@foxmail.com)
 */
public final class Pages {

    public static <T> Page<T> doSelectPage(int pageNum, int pageSize, Supplier<List<T>> listAction) {
        return doSelectPage(pageNum, pageSize, listAction, null);
    }

    public static <T> Page<T> doSelectPage(int pageNum, int pageSize, Supplier<List<T>> listAction, int total) {
        return doSelectPage(pageNum, pageSize, listAction, () -> (long) total);
    }

    public static <T> Page<T> doSelectPage(int pageNum, int pageSize, Supplier<List<T>> listAction, long total) {
        return doSelectPage(pageNum, pageSize, listAction, () -> total);
    }

    public static <T> Page<T> doSelectPage(int pageNum, int pageSize, Supplier<List<T>> listAction, Supplier<Long> countAction) {
        boolean autoCount = countAction == null;
        com.github.pagehelper.Page<T> page = PageHelper.startPage(pageNum, pageSize, autoCount)
                .doSelectPage(listAction::get);
        long total = autoCount ? page.getTotal() : countAction.get();
        Page<T> result = new Page<>();
        result.setItems(page.getResult());
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setTotal(total);
        result.setPages(calcPages(total, pageSize));
        result.setOffset(calcStartRow(pageNum, pageSize));
        return result;
    }

    public static int calcPages(int total, int pageSize) {
        if (total <= 0 || pageSize <= 0) {
            return 0;
        } else {
            int pages = total / pageSize;
            if (total % pageSize != 0) {
                ++pages;
            }
            return pages;
        }
    }

    public static int calcPages(long total, int pageSize) {
        return calcPages((int) total, pageSize);
    }

    public static long calcStartRow(int pageNum, int pageSize) {
        if (pageNum <= 0) {
            return 0L;
        }
        return (long) (pageNum - 1) * pageSize;
    }
}
