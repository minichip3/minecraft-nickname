package net.minichip.minecraftnickname;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

public class MinecraftNickname extends JavaPlugin implements Listener {

    // 닉네임 데이터: UUID -> JSONObject (형식: { "name": "실제이름", "nick": "닉네임" })
    private Map<String, JSONObject> nicknames = new HashMap<>();
    private File configFile;
    private File nicknameFile;

    @Override
    public void onEnable() {
        getLogger().info("MinecraftNickname Plugin이 활성화되었습니다!");

        // 플러그인 폴더 생성
        File pluginDir = getDataFolder();
        if (!pluginDir.exists()) {
            pluginDir.mkdirs();
        }

        // config.yml 및 nicknames.json 파일 설정
        configFile = new File(pluginDir, "config.yml");
        if (!configFile.exists()) {
            createDefaultConfig();
        }
        loadConfig();
        loadNicknames();

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("nickreload").setExecutor(this);
        getCommand("nickset").setExecutor(this);
        getCommand("nickadd").setExecutor(this);
        getCommand("nickmodify").setExecutor(this);
        getCommand("nickdel").setExecutor(this);

        // 서버에 접속 중인 모든 플레이어의 탭 리스트 닉네임 적용
        applyNicknamesToAllPlayers();
    }

    @Override
    public void onDisable() {
        getLogger().info("MinecraftNickname Plugin이 비활성화되었습니다.");
    }

    private void createDefaultConfig() {
        try {
            getLogger().warning("config.yml 파일이 존재하지 않습니다. 기본 파일을 생성합니다.");
            configFile.createNewFile();
            String defaultConfig = "# 닉네임 JSON 파일의 경로를 설정하세요.\n"
                    + "# 예: nickname-file-path: \"nicknames.json\"\n";
            Files.write(configFile.toPath(), defaultConfig.getBytes());
        } catch (IOException e) {
            getLogger().severe("config.yml 파일 생성 중 오류 발생: " + e.getMessage());
        }
    }

    private void loadConfig() {
        if (!configFile.exists()) {
            getLogger().warning("config.yml 파일을 찾을 수 없습니다!");
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        String filePath = config.getString("nickname-file-path", "nicknames.json");
        if (filePath.startsWith("/") || filePath.contains(":")) {
            nicknameFile = new File(filePath);
        } else {
            nicknameFile = new File(getDataFolder(), filePath);
        }
        getLogger().info("닉네임 JSON 파일 경로: " + nicknameFile.getAbsolutePath());
    }

    private void loadNicknames() {
        if (!nicknameFile.exists()) {
            getLogger().warning("nicknames.json 파일이 존재하지 않습니다. 빈 파일을 생성합니다.");
            try {
                nicknameFile.createNewFile();
                Files.write(nicknameFile.toPath(), "{}".getBytes());
            } catch (IOException e) {
                getLogger().severe("nicknames.json 파일 생성 중 오류 발생: " + e.getMessage());
            }
        }
        try {
            String content = new String(Files.readAllBytes(nicknameFile.toPath()));
            JSONObject json = new JSONObject(content);
            nicknames.clear();
            for (String key : json.keySet()) {
                nicknames.put(key, json.getJSONObject(key));
            }
            getLogger().info("닉네임 데이터 로드 완료: " + nicknames);
        } catch (IOException e) {
            getLogger().severe("nicknames.json 파일 읽는 중 오류 발생: " + e.getMessage());
        }
    }

    // 채팅 메시지에서 탭 리스트에만 닉네임 적용 (머리 위 제거)
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        JSONObject data = nicknames.get(uuid);
        String nickname = (data != null && data.has("nick")) ? data.getString("nick") : null;
        String message = event.getMessage().replace("%", "%%");
        String format = (nickname != null)
                ? ChatColor.AQUA + "[" + nickname + "] " + ChatColor.GOLD + "<" + player.getName() + "> " + ChatColor.WHITE + message
                : ChatColor.GOLD + "<" + player.getName() + "> " + ChatColor.WHITE + message;
        event.setFormat(format);
    }

    // 모든 온라인 플레이어에 대해 탭 리스트 닉네임 적용
    private void applyNicknamesToAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyNicknameToPlayer(player);
        }
    }

    private void applyNicknameToPlayer(Player player) {
        String uuid = player.getUniqueId().toString();
        if (nicknames.containsKey(uuid)) {
            JSONObject data = nicknames.get(uuid);
            String nick = data.optString("nick", null);
            String actualName = data.optString("name", player.getName());
            if (nick != null) {
                player.setPlayerListName(ChatColor.AQUA + "[" + nick + "] " + ChatColor.RESET + actualName);
            }
        } else {
            player.setPlayerListName(ChatColor.RESET + player.getName());
        }
    }

    private void saveNicknames() {
        try {
            JSONObject json = new JSONObject(nicknames);
            Files.write(nicknameFile.toPath(), json.toString(4).getBytes());
            getLogger().info("닉네임 데이터가 업데이트되었습니다.");
        } catch (IOException e) {
            getLogger().severe("nicknames.json 파일 저장 중 오류 발생: " + e.getMessage());
        }
    }

    // --- 명령어 처리 ---
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /nickreload: 닉네임 파일 재로드
        if (command.getName().equalsIgnoreCase("nickreload")) {
            loadNicknames();
            sender.sendMessage(ChatColor.GREEN + "닉네임 데이터가 다시 로드되었습니다.");
            applyNicknamesToAllPlayers();
            return true;
        }

        // /nickset: 플레이어가 자신의 닉네임 설정 (자신의 UUID 사용)
        if (command.getName().equalsIgnoreCase("nickset")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "이 명령어는 플레이어만 사용할 수 있습니다.");
                return true;
            }
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "사용법: /nickset <닉네임>");
                return true;
            }
            Player player = (Player) sender;
            String uuid = player.getUniqueId().toString();
            String newNickname = args[0];
            if (nicknames.containsKey(uuid)) {
                sender.sendMessage(ChatColor.RED + "이미 닉네임이 설정되어 있습니다! 닉네임을 변경하려면 관리자에게 문의하세요.");
                return true;
            }
            // 중복 닉네임 검사
            for (JSONObject obj : nicknames.values()) {
                if (obj.optString("nick", "").equalsIgnoreCase(newNickname)) {
                    sender.sendMessage(ChatColor.RED + newNickname + "은 이미 사용 중인 닉네임입니다!");
                    return true;
                }
            }
            JSONObject data = new JSONObject();
            data.put("name", player.getName());
            data.put("nick", newNickname);
            nicknames.put(uuid, data);
            saveNicknames();
            sender.sendMessage(ChatColor.GREEN + "닉네임이 '" + newNickname + "'(으)로 설정되었습니다!");
            applyNicknamesToAllPlayers();
            return true;
        }

        // /nickadd: 관리자용, 플레이어 이름으로 UUID를 조회 후 닉네임 추가
        if (command.getName().equalsIgnoreCase("nickadd")) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "사용법: /nickadd <플레이어 이름> <닉네임>");
                return true;
            }
            String targetName = args[0];
            String newNickname = args[1];
            // 플레이어 이름으로 UUID 조회 (MojangAPI.getUUID 가 구현되어 있어야 합니다)
            String uuid = MojangAPI.getUUID(targetName);
            if (uuid == null) {
                sender.sendMessage(ChatColor.RED + targetName + "의 UUID를 찾을 수 없습니다!");
                return true;
            }
            if (nicknames.containsKey(uuid)) {
                sender.sendMessage(ChatColor.RED + targetName + "의 닉네임이 이미 존재합니다!");
                return true;
            }
            // 중복 닉네임 검사
            for (JSONObject obj : nicknames.values()) {
                if (obj.optString("nick", "").equalsIgnoreCase(newNickname)) {
                    sender.sendMessage(ChatColor.RED + newNickname + "은 이미 사용 중인 닉네임입니다!");
                    return true;
                }
            }
            JSONObject data = new JSONObject();
            data.put("name", targetName);
            data.put("nick", newNickname);
            nicknames.put(uuid, data);
            saveNicknames();
            sender.sendMessage(ChatColor.GREEN + targetName + "의 닉네임이 '" + newNickname + "'으로 추가되었습니다!");
            applyNicknamesToAllPlayers();
            return true;
        }

        // /nickmodify: 관리자용, 플레이어 이름 또는 기존 닉네임으로 수정 (플레이어 이름을 기준으로 UUID 조회)
        if (command.getName().equalsIgnoreCase("nickmodify")) {
            if (!sender.hasPermission("nickname.manage")) {
                sender.sendMessage(ChatColor.RED + "이 명령어를 실행할 권한이 없습니다.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "사용법: /nickmodify <플레이어 이름 또는 기존 닉네임> <새 닉네임>");
                return true;
            }
            String target = args[0];
            String newNickname = args[1];
            // 중복 닉네임 검사
            for (JSONObject obj : nicknames.values()) {
                if (obj.optString("nick", "").equalsIgnoreCase(newNickname)) {
                    sender.sendMessage(ChatColor.RED + "이미 사용 중인 닉네임입니다!");
                    return true;
                }
            }
            boolean found = false;
            String uuid = MojangAPI.getUUID(target);
            if (uuid != null && nicknames.containsKey(uuid)) {
                JSONObject data = nicknames.get(uuid);
                data.put("nick", newNickname);
                found = true;
            } else {
                // 파일에서 기존 닉네임이나 이름을 기준으로 검색
                for (Map.Entry<String, JSONObject> entry : nicknames.entrySet()) {
                    JSONObject data = entry.getValue();
                    if (data.optString("nick", "").equalsIgnoreCase(target) ||
                        data.optString("name", "").equalsIgnoreCase(target)) {
                        data.put("nick", newNickname);
                        uuid = entry.getKey();
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                sender.sendMessage(ChatColor.RED + "해당 플레이어나 닉네임을 찾을 수 없습니다.");
                return true;
            }
            saveNicknames();
            sender.sendMessage(ChatColor.GREEN + target + "의 닉네임이 '" + newNickname + "'으로 변경되었습니다!");
            applyNicknamesToAllPlayers();
            return true;
        }

        // /nickdel: 관리자용, 플레이어 이름 또는 닉네임으로 삭제 (플레이어 이름 기준으로 UUID 조회)
        if (command.getName().equalsIgnoreCase("nickdel")) {
            if (!sender.hasPermission("nickname.manage")) {
                sender.sendMessage(ChatColor.RED + "이 명령어를 실행할 권한이 없습니다.");
                return true;
            }
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "사용법: /nickdel <플레이어 이름 또는 닉네임>");
                return true;
            }
            String target = args[0];
            boolean found = false;
            String uuid = MojangAPI.getUUID(target);
            if (uuid != null && nicknames.containsKey(uuid)) {
                nicknames.remove(uuid);
                found = true;
            } else {
                String keyToRemove = null;
                for (Map.Entry<String, JSONObject> entry : nicknames.entrySet()) {
                    JSONObject data = entry.getValue();
                    if (data.optString("nick", "").equalsIgnoreCase(target) ||
                        data.optString("name", "").equalsIgnoreCase(target)) {
                        keyToRemove = entry.getKey();
                        break;
                    }
                }
                if (keyToRemove != null) {
                    nicknames.remove(keyToRemove);
                    found = true;
                    target = keyToRemove;
                }
            }
            if (!found) {
                sender.sendMessage(ChatColor.RED + target + "의 닉네임이 존재하지 않습니다!");
                return true;
            }
            saveNicknames();
            sender.sendMessage(ChatColor.GREEN + target + "의 닉네임이 삭제되었습니다!");
            applyNicknamesToAllPlayers();
            return true;
        }

        return false;
    }
}