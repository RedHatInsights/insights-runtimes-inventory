/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.models;

import java.util.ArrayList;

public sealed interface InsightsMessage permits JvmInstance, UpdateInstance {

  // This will sanitize the message by redacting any sensitive information that we don't want to
  // persist
  void sanitize();

  /**
   * Sanitizes a string that contains java style parameters of the type -Dxxxxx=yyyyy by
   * substituting the yyyyy value for an obfuscated string
   *
   * @param parameters
   * @return a sanitized parameter string suitable for persisting
   */
  static String sanitizeJavaParameters(final String parameters) {
    final StringBuilder out = new StringBuilder();
    String redacted = "=*****"; // What to replace sanitized content with

    for (final String token : tokenizeComplexJavaParameters(parameters)) {
      // We only care about -Dxxxxx=yyyyy params
      if (token.startsWith("-D") && token.contains("=")) {
        String[] parts = token.split("=", 2);
        out.append(parts[0]);
        out.append(redacted);
        // We might be parsing json
        // if so, preserve the list comma or list closing bracket
        if (token.endsWith(",")) {
          out.append(',');
        }
        if (token.endsWith("]")) {
          out.append(']');
        }
      } else {
        out.append(token);
      }
      out.append(" ");
    }
    // Remove the last added space
    out.deleteCharAt(out.length() - 1);
    return out.toString();
  }

  // This tokenizes a string, but with some special rules
  // It tokenizes based on spaces, but it will interpret quotes
  // that start in the middle of a string, after an '='
  // This is important because some of the data we want to preserve might
  // look like -Dxxxxx="this is all one token"
  // This is also aware of escape sequences
  static String[] tokenizeComplexJavaParameters(final String parameters) {
    final ArrayList<String> tokens = new ArrayList<String>();
    StringBuilder currentWord = new StringBuilder();
    Character currentQuote = null;
    boolean escaping = false;
    boolean afterEquals = false;
    // Order is important here. Rearrange at your own risk.
    for (final char c : parameters.toCharArray()) {
      // If we're not escaping, start escaping and continue
      if (c == '\\' && !escaping) {
        escaping = true;
        currentWord.append(c);
        continue;
      }

      // If we're escaping, always just add to the word and continue
      if (escaping) {
        escaping = false;
        currentWord.append(c);
        continue;
      }

      // If we see an '=', remember that and continue
      if (c == '=') {
        afterEquals = true;
        currentWord.append(c);
        continue;
      }

      // If we're not in a quote and we hit a space, save the word and continue
      if (currentQuote == null && c == ' ') {
        tokens.add(currentWord.toString());
        currentWord = new StringBuilder();
        continue;
      }

      // If we see a quote...
      if (c == '\'' || c == '"') {
        // If we are quoting...
        if (currentQuote != null) {
          // stop quoting if we're at the matching quote
          if (c == currentQuote) {
            currentQuote = null;
          }
        } else {
          // So we're not quoting...
          // If we're at a new word or after an equals, start quoting
          if (afterEquals || currentWord.isEmpty()) {
            currentQuote = c;
          }
        }
      }

      // Otherwise, just add the char
      afterEquals = false;
      currentWord.append(c);
    }
    // Add the last word for the end of string
    tokens.add(currentWord.toString());
    return tokens.toArray(new String[0]);
  }
}
