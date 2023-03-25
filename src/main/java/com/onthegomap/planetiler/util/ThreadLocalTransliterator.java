package com.onthegomap.planetiler.util;

import com.ibm.icu.text.Transliterator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A {@link com.ibm.icu.text.Transliterator} that does not share any static data with other thread local
 * transliterators.
 * <p>
 * By default, {@link com.ibm.icu.text.Transliterator} synchronizes on static data during transliteration, which results
 * in contention between threads when transliterating many strings in parallel. Separate instances of this class can be
 * used across different threads in order to transliterate without contention.
 */
public class ThreadLocalTransliterator {

    /**
     * Returns a {@link com.ibm.icu.text.Transliterator} for {@code id} that does not share any data with transliterators
     * on other threads.
     */
    public TransliteratorInstance getInstance(String id) {
        Transliterator t = Transliterator.getInstance(id);
        return t::transliterate;
    }

    @FunctionalInterface
    public interface TransliteratorInstance {
        String transliterate(String input);
    }
}
