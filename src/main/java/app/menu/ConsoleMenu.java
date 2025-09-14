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
    // Record representing a single menu item (label + action to run)
    private record Item(String label, Runnable action) {}

    private final String title;          // Title displayed at the top of the menu
    private final List<Item> items = new ArrayList<>(); // List of all menu items
    private final Scanner in = new Scanner(System.in);  // Scanner for reading user input

    // Constructor: initializes menu with a title
    public ConsoleMenu(String title) { this.title = title; }

    /**
     * Adds an option to the menu.
     * @param label  Text shown in the menu list
     * @param action Runnable executed when the option is selected
     * @return this (to allow chaining multiple options)
     */
    public ConsoleMenu option(String label, Runnable action) {
        items.add(new Item(label, action));
        return this;
    }

    /**
     * Main loop: repeatedly displays menu, reads user input,
     * and runs the selected option.
     */
    public void loop() {
        while (true) {
            // Print menu header
            System.out.println("------ " + title + " ------");

            // Print numbered menu options
            for (int i = 0; i < items.size(); i++) {
                System.out.printf("%d. %s%n", i + 1, items.get(i).label());
            }

            // Prompt for user input
            System.out.print("> ");
            String s = in.nextLine().trim();

            // Parse input into an integer (ignore invalid input)
            int choice;
            try {
                choice = Integer.parseInt(s);
            } catch (Exception e) {
                continue; // If parsing fails, re-display menu
            }

            // Ignore out-of-range numbers
            if (choice < 1 || choice > items.size()) continue;

            // Run the selected menu option
            items.get(choice - 1).action().run();
        }
    }

    /**
     * Static helper to exit the program immediately.
     */
    public static void exit() { System.exit(0); }

    /**
     * Static helper to prompt the user for a string input.
     * @param label Label shown before input
     * @return trimmed user input string
     */
    public static String prompt(String label) {
        System.out.print(label + ": ");
        return new Scanner(System.in).nextLine().trim();
    }

    /**
     * Static helper to prompt the user for a file path.
     * Shows the current path as default, keeps it if input is empty.
     * @param label   Label shown before input
     * @param current Current path to show as default
     * @return Path chosen by user (or the current one if input is empty)
     */
    public static Path promptPath(String label, Path current) {
        System.out.printf("%s (current: %s): ", label, current);
        String s = new Scanner(System.in).nextLine().trim();
        return s.isEmpty() ? current : Path.of(s);
    }
}
