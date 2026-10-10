/*
 * Copyright (c) 2018-2026, Thomas Meaney
 * Copyright (c) contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.eintosti.buildsystem.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.eintosti.buildsystem.util.UpdateChecker.SpigotRequest;
import de.eintosti.buildsystem.util.UpdateChecker.UpdateReason;
import de.eintosti.buildsystem.util.UpdateChecker.UpdateResult;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class UpdateCheckerTest {

    private final List<Runnable> submitted = new ArrayList<>();

    @Test
    void repeatedChecks_shareOneRequest() {
        UpdateChecker checker = checker(respondWith("{\"current_version\": \"1.0\"}"));

        CompletableFuture<UpdateResult> first = checker.requestUpdateCheck();
        assertSame(first, checker.requestUpdateCheck());
        runSubmitted();

        assertEquals(UpdateReason.UP_TO_DATE, first.join().getReason());
        assertSame(first, checker.requestUpdateCheck());
        assertEquals(1, submitted.size());
    }

    @Test
    void failedCheck_isRetried() {
        UpdateChecker checker = checker(() -> {
            throw new IOException("offline");
        });

        CompletableFuture<UpdateResult> first = checker.requestUpdateCheck();
        runSubmitted();

        assertEquals(UpdateReason.COULD_NOT_CONNECT, first.join().getReason());
        assertNotSame(first, checker.requestUpdateCheck());
    }

    @Test
    void responseWithoutAVersion_isAnUnknownErrorAndRetried() {
        UpdateChecker checker = checker(respondWith("{}"));

        CompletableFuture<UpdateResult> first = checker.requestUpdateCheck();
        runSubmitted();

        assertEquals(UpdateReason.UNKNOWN_ERROR, first.join().getReason());
        assertNotSame(first, checker.requestUpdateCheck());
    }

    private UpdateChecker checker(SpigotRequest request) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDescription()).thenReturn(new PluginDescriptionFile("BuildSystem", "1.0", "Main"));
        return new UpdateChecker(plugin, request, submitted::add);
    }

    @SuppressWarnings("unchecked")
    private static SpigotRequest respondWith(String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(body);
        return () -> response;
    }

    private void runSubmitted() {
        List.copyOf(submitted).forEach(Runnable::run);
    }
}
