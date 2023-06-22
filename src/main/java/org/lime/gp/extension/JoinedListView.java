package org.lime.gp.extension;

import org.jetbrains.annotations.NotNull;
import org.lime.system;

import java.util.*;
import java.util.stream.Stream;

public class JoinedListView<T> implements List<T> {
    private final List<system.Func0<List<T>>> lists;
    private JoinedListView(Iterable<system.Func0<List<T>>> lists) {
        this.lists = new ArrayList<>();
        lists.forEach(this.lists::add);
    }
    private JoinedListView(List<system.Func0<List<T>>> lists) {
        this.lists = lists;
    }

    public static <T>JoinedListView<T> of(Iterable<List<T>> lists) {
        List<system.Func0<List<T>>> _lists = new ArrayList<>();
        lists.forEach(item -> _lists.add(() -> item));
        return new JoinedListView<>(_lists);
    }
    public static <T>JoinedListView<T> of(List<T> singleList) { return of(Collections.singleton(singleList)); }
    public static <T>JoinedListView<T> of(List<T>... lists) { return of((Iterable<List<T>>)Arrays.asList(lists)); }

    public JoinedListView<T> append(int index, system.Func0<List<T>> list) { lists.add(index, list); return this; }
    public JoinedListView<T> append(int index, List<T> list) { lists.add(index, () -> list); return this; }
    public JoinedListView<T> append(system.Func0<List<T>> list) { lists.add(list); return this; }
    public JoinedListView<T> append(List<T> list) { lists.add(() -> list); return this; }

    private Stream<List<T>> listStream() { return lists.stream().map(system.Func0::invoke); }

    private <I>I ExecuteAction(I init, system.Func2<List<T>, I, I> step) {
        for (system.Func0<List<T>> list : lists) init = step.invoke(list.invoke(), init);
        return init;
    }
    private <I>I ExecuteAction(I init, system.Func3<List<T>, Integer, I, I> step) {
        int i = 0;
        for (var _list : lists) {
            List<T> list = _list.invoke();
            init = step.invoke(list, i, init);
            i += list.size();
        }
        return init;
    }
    private <I>I ExecuteAction(I init, system.Func4<List<T>, Integer, Integer, I, I> step) {
        int i = 0;
        for (var _list : lists) {
            List<T> list = _list.invoke();
            int length = list.size();
            init = step.invoke(list, i, length, init);
            i += length;
        }
        return init;
    }

    @Override public int size() { return ExecuteAction(0, (list, v) -> list.size() + v); }
    @Override public boolean isEmpty() { return ExecuteAction(true, (list, v) -> list.isEmpty() && v); }
    @Override public boolean contains(Object o) { return ExecuteAction(false, (list, v) -> v || list.contains(o)); }
    @Override public Object[] toArray() { return this.stream().toArray(); }
    @NotNull @Override public <T1> T1[] toArray(@NotNull T1[] a) { return this.stream().toArray(count -> Arrays.copyOf(a, count)); }
    @Override public boolean containsAll(@NotNull Collection<?> c) {
        if (c.isEmpty()) return true;
        Set<?> set = new HashSet<>(c);
        return this.stream().anyMatch(s -> set.remove(s) && set.isEmpty());
    }
    @Override public T get(int index) {
        return ExecuteAction(null, (list, offset, length, v) -> {
            if (v != null) return v;
            int i = index - offset;
            return i >= 0 && i < length ? list.get(i) : null;
        });
    }
    @Override public int indexOf(Object o) {
        return ExecuteAction(-1, (list, offset, length, v) -> {
            if (v != -1) return v;
            int index = list.indexOf(o);
            return index == -1 ? -1 : (offset + index);
        });
    }

    @Override public int lastIndexOf(Object o) { return ExecuteAction(-1, (list, offset, length, v) -> Math.max(v, list.lastIndexOf(o))); }
    @NotNull @Override public Iterator<T> iterator() { return listIterator(); }
    @NotNull @Override public ListIterator<T> listIterator() { return listIterator(0); }
    @NotNull @Override public ListIterator<T> listIterator(int index) {

        return new ListIterator<T>() {
            private int cursor = index;
            private final int size = size();
            @Override public boolean hasNext() { return cursor != size; }
            @Override public T next() { T value = get(cursor); cursor++; return value; }
            @Override public boolean hasPrevious() { return cursor != 1; }
            @Override public T previous() { cursor--; return get(cursor); }
            @Override public int nextIndex() { return cursor; }
            @Override public int previousIndex() { return cursor - 1; }
            @Override public void remove() { throw new UnsupportedOperationException(); }
            @Override public void set(T t) { throw new UnsupportedOperationException(); }
            @Override public void add(T t) { throw new UnsupportedOperationException(); }
        };
    }

    @NotNull @Override public List<T> subList(int fromIndex, int toIndex) { throw new UnsupportedOperationException(); }


    @Override public boolean add(T t) { throw new UnsupportedOperationException(); }
    @Override public void add(int index, T element) { throw new UnsupportedOperationException(); }
    @Override public boolean addAll(@NotNull Collection<? extends T> c) { throw new UnsupportedOperationException(); }
    @Override public boolean addAll(int index, @NotNull Collection<? extends T> c) { throw new UnsupportedOperationException(); }
    @Override public T set(int index, T element) { throw new UnsupportedOperationException(); }
    @Override public boolean remove(Object o) { throw new UnsupportedOperationException(); }
    @Override public T remove(int index) { throw new UnsupportedOperationException(); }
    @Override public boolean removeAll(@NotNull Collection<?> c) { throw new UnsupportedOperationException(); }
    @Override public boolean retainAll(@NotNull Collection<?> c) { throw new UnsupportedOperationException(); }
    @Override public void clear() { throw new UnsupportedOperationException(); }
}
