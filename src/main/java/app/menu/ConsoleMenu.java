package app.menu;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Tiny console menu helper shared by the three apps.
 * - Add options with .option(label, Runnable)
 * - Call .loop() to render and handle user input.
 */
public final class ConsoleMenu {
    private record Item(String label, Runnable action) {}
    private final String title;
    private final List<Item> items = new ArrayList<>();
    private final Scanner in = new Scanner(System.in);

    public ConsoleMenu(String title) { this.title = title; }

    public ConsoleMenu option(String label, Runnable action) {
        items.add(new Item(label, action));
        return this;
    }

    public void loop() {
        while (true) {
            System.out.println("------ " + title + " ------");
            for (int i = 0; i < items.size(); i++) {
                System.out.printf("%d. %s%n", i + 1, items.get(i).label());
            }
            System.out.print("> ");
            String s = in.nextLine().trim();
            int choice;
            try { choice = Integer.parseInt(s); } catch (Exception e) { continue; }
            if (choice < 1 || choice > items.size()) continue;
            items.get(choice - 1).action().run();
        }
    }

    public static void exit() { System.exit(0); }

    public static String prompt(String label) {
        System.out.print(label + ": ");
        return new Scanner(System.in).nextLine().trim();
    }

    public static Path promptPath(String label, Path current) {
        System.out.printf("%s (current: %s): ", label, current);
        String s = new Scanner(System.in).nextLine().trim();
        return s.isEmpty() ? current : Path.of(s);
    }
}
