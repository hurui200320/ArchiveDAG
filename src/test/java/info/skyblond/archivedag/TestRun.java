package info.skyblond.archivedag;

import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.Comparator;

public class TestRun {
    public static void main(String[] args) {
        Arrays.stream(HttpStatus.values()).sorted(Comparator.comparingInt(HttpStatus::value))
                .forEach(s -> {
                    System.out.println("    // " + s.value() + " - " + s.getReasonPhrase());
                });
    }
}
