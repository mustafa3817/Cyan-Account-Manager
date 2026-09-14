package com.mustafa.accountswitcher.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AccountStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<Account> ACCOUNTS = new ArrayList<>();
    private static String activeUuid = null;

    private static File getConfigFile() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("account-switcher");
        File dir = configDir.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, "accounts.json");
    }

    public static synchronized void load() {
        ACCOUNTS.clear();
        File file = getConfigFile();
        if (!file.exists()) return;

        try (java.io.Reader reader = new java.io.InputStreamReader(new java.io.FileInputStream(file), java.nio.charset.StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has("activeUuid")) {
                activeUuid = root.get("activeUuid").getAsString();
            }
            if (root.has("accounts")) {
                JsonArray array = root.getAsJsonArray("accounts");
                for (JsonElement element : array) {
                    if (element.isJsonObject()) {
                        ACCOUNTS.add(Account.fromJson(element.getAsJsonObject()));
                    }
                }
            }
            System.out.println("[AccountSwitcher] " + ACCOUNTS.size() + " hesap basariyla yuklendi.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (ACCOUNTS.isEmpty()) {
            importFromOpsecFile();
        }
    }

    public static synchronized void save() {
        File file = getConfigFile();
        try (java.io.Writer writer = new java.io.OutputStreamWriter(new java.io.FileOutputStream(file), java.nio.charset.StandardCharsets.UTF_8)) {
            JsonObject root = new JsonObject();
            if (activeUuid != null) {
                root.addProperty("activeUuid", activeUuid);
            }
            JsonArray array = new JsonArray();
            for (Account account : ACCOUNTS) {
                array.add(account.toJson());
            }
            root.add("accounts", array);
            GSON.toJson(root, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized List<Account> getAccounts() {
        return new ArrayList<>(ACCOUNTS);
    }

    public static synchronized void addAccount(Account newAccount) {
        ACCOUNTS.removeIf(a -> a.getUuid().equalsIgnoreCase(newAccount.getUuid()));
        ACCOUNTS.add(0, newAccount);
        activeUuid = newAccount.getUuid();
        save();
    }

    public static synchronized void removeAccount(Account account) {
        ACCOUNTS.removeIf(a -> a.getUuid().equalsIgnoreCase(account.getUuid()));
        if (activeUuid != null && activeUuid.equalsIgnoreCase(account.getUuid())) {
            activeUuid = ACCOUNTS.isEmpty() ? null : ACCOUNTS.get(0).getUuid();
        }
        save();
    }

    public static synchronized String getActiveUuid() {
        return activeUuid;
    }

    public static synchronized void setActiveUuid(String uuid) {
        activeUuid = uuid;
        save();
    }

    public static synchronized String exportToJsonString() {
        JsonObject root = new JsonObject();
        JsonArray array = new JsonArray();
        for (Account account : ACCOUNTS) {
            array.add(account.toJson());
        }
        root.add("accounts", array);
        return GSON.toJson(root);
    }

    public static synchronized int importFromJsonString(String jsonString) {
        if (jsonString == null || jsonString.isBlank()) return 0;
        int importedCount = 0;
        try {
            JsonElement parsed = JsonParser.parseString(jsonString.trim());
            JsonArray array = null;
            if (parsed.isJsonObject() && parsed.getAsJsonObject().has("accounts")) {
                array = parsed.getAsJsonObject().getAsJsonArray("accounts");
            } else if (parsed.isJsonArray()) {
                array = parsed.getAsJsonArray();
            }

            if (array != null) {
                for (JsonElement element : array) {
                    if (element.isJsonObject()) {
                        JsonObject obj = element.getAsJsonObject();
                        Account acc = Account.fromJson(obj);
                        if (acc.getUuid() != null && !acc.getUuid().isBlank()) {
                            ACCOUNTS.removeIf(a -> a.getUuid().equalsIgnoreCase(acc.getUuid()));
                            ACCOUNTS.add(acc);
                            importedCount++;
                        }
                    }
                }
                save();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return importedCount;
    }

    public static synchronized int importFromOpsecFile() {
        try {
            Path opsecPath = FabricLoader.getInstance().getConfigDir().resolve("opsec-accounts.json");
            File opsecFile = opsecPath.toFile();
            if (opsecFile.exists()) {
                String content = java.nio.file.Files.readString(opsecPath, java.nio.charset.StandardCharsets.UTF_8);
                return importFromJsonString(content);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public static synchronized boolean moveUp(Account account) {
        if (account == null) return false;
        int index = ACCOUNTS.indexOf(account);
        if (index > 0) {
            Account removed = ACCOUNTS.remove(index);
            ACCOUNTS.add(index - 1, removed);
            save();
            return true;
        }
        return false;
    }

    public static synchronized boolean moveDown(Account account) {
        if (account == null) return false;
        int index = ACCOUNTS.indexOf(account);
        if (index >= 0 && index < ACCOUNTS.size() - 1) {
            Account removed = ACCOUNTS.remove(index);
            ACCOUNTS.add(index + 1, removed);
            save();
            return true;
        }
        return false;
    }
}
