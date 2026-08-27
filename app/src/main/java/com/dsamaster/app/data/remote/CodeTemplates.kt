package com.dsamaster.app.data.remote

object CodeTemplates {
    val PYTHON = """
        import sys

        def solve():
            data = sys.stdin.read().split('|')
            # TODO: parse `data` (matches the pipe-separated Input shown above)
            # and implement your solution. Print the final answer.
            pass

        solve()
    """.trimIndent()

    val JAVA = """
        import java.util.*;

        public class Main {
            public static void main(String[] args) {
                Scanner scanner = new Scanner(System.in);
                String input = scanner.hasNextLine() ? scanner.nextLine() : "";
                String[] data = input.split("\\|");
                // TODO: parse `data` and implement your solution. Print the final answer.
            }
        }
    """.trimIndent()

    val CPP = """
        #include <bits/stdc++.h>
        using namespace std;

        int main() {
            string line;
            getline(cin, line);
            // TODO: split `line` on '|' and implement your solution.
            // Print the final answer.
            return 0;
        }
    """.trimIndent()

    fun forLanguage(language: String): String = when (language) {
        "python" -> PYTHON
        "java" -> JAVA
        "cpp" -> CPP
        else -> ""
    }
}