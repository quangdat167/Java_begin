package vn.dangquangdat.javabegin.learning.day04;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Ngay 4: List, Set, Map, generic, equals/hashCode va do phuc tap co ban. */
public class CollectionsDemo {

    public static void main(String[] args) {
        ConversationStore<Long, Conversation> store = new ConversationStore<>();
        store.save(2L, new Conversation(2L, "Java interview", Set.of("java", "career")));
        store.save(1L, new Conversation(1L, "Spring Security", Set.of("java", "security")));

        List<Conversation> sorted = new ArrayList<>(store.values());
        sorted.sort(Comparator.comparing(Conversation::title));

        Set<String> uniqueTags = new LinkedHashSet<>();
        sorted.forEach(conversation -> uniqueTags.addAll(conversation.tags()));

        System.out.println("Sorted conversations: " + sorted);
        System.out.println("Unique tags: " + uniqueTags);
    }

    static final class ConversationStore<K, V> {
        private final Map<K, V> data = new HashMap<>();

        void save(K id, V value) {
            data.put(id, value); // HashMap put/get trung binh O(1)
        }

        V find(K id) {
            return data.get(id);
        }

        List<V> values() {
            return List.copyOf(data.values());
        }
    }

    record Conversation(long id, String title, Set<String> tags) {
        Conversation {
            tags = Set.copyOf(tags); // defensive copy: khong de ben ngoai sua noi bo object
        }
    }
}

