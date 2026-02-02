package com.test.Database_Testing;

import com.jcraft.jsch.*;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class Brain_Backup_Sanity {

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter brain number (e.g., 222): ");
        String brainNumber = scanner.nextLine().trim();

        String user = "hbp";
        String password = "Health#123";

        String host1 = "pp6.humanbrain.in";
        String host2 = "ap7.humanbrain.in";

        String path1 = "/mnt/remote/analytics/" + brainNumber;
        String path2 = "/mnt/local/store/repos1/iitlab/humanbrain/analytics/" + brainNumber;

        // TOTAL SIZE & FILE COUNT
        System.out.println("\n📦 Comparing TOTAL storage and file count for brain: " + brainNumber);

        String totalSize1 = runCommand(user, password, host1, "du -sh " + path1 + " | cut -f1");
        String totalSize2 = runCommand(user, password, host2, "du -sh " + path2 + " | cut -f1");

        String totalFiles1 = runCommand(user, password, host1, "find " + path1 + " -type f | wc -l");
        String totalFiles2 = runCommand(user, password, host2, "find " + path2 + " -type f | wc -l");

        System.out.println("🔹 Total Size:  " + totalSize1.trim() + " vs " + totalSize2.trim());
        System.out.println("🔹 Total Files: " + totalFiles1.trim() + " vs " + totalFiles2.trim());

        // INDIVIDUAL SUBFOLDER COMPARISON
        System.out.println("\n📁 Comparing individual subfolders in: " + brainNumber + "\n");

        List<String> folders1 = listSubfolders(user, password, host1, path1);
        List<String> folders2 = listSubfolders(user, password, host2, path2);
        Set<String> commonFolders = new TreeSet<>(folders1);
        commonFolders.retainAll(folders2);

        for (String folder : commonFolders) {
            System.out.println("🔄 Checking folder: " + folder + "...");

            String subPath1 = path1 + "/" + folder;
            String subPath2 = path2 + "/" + folder;

            String size1 = runCommand(user, password, host1, "du -sh " + subPath1 + " | cut -f1");
            String size2 = runCommand(user, password, host2, "du -sh " + subPath2 + " | cut -f1");

            String count1 = runCommand(user, password, host1, "find " + subPath1 + " -type f | wc -l");
            String count2 = runCommand(user, password, host2, "find " + subPath2 + " -type f | wc -l");

            boolean sizeMatch = size1.trim().equals(size2.trim());
            boolean countMatch = count1.trim().equals(count2.trim());

            System.out.println("\n📂 " + folder);
            System.out.println("   Size:  " + size1.trim() + " vs " + size2.trim() + " → " + (sizeMatch ? "✅ Match" : "❌ Mismatch"));
            System.out.println("   Files: " + count1.trim() + " vs " + count2.trim() + " → " + (countMatch ? "✅ Match" : "❌ Mismatch"));

            // If files mismatch → show missing
            if (!countMatch) {
                Set<String> files1 = getRelativeFiles(user, password, host1, subPath1);
                Set<String> files2 = getRelativeFiles(user, password, host2, subPath2);

                Set<String> missingOnHost2 = new TreeSet<>(files1);
                missingOnHost2.removeAll(files2);

                Set<String> missingOnHost1 = new TreeSet<>(files2);
                missingOnHost1.removeAll(files1);

                if (!missingOnHost2.isEmpty()) {
                    System.out.println("   ❌ Missing on " + host2 + ":");
                    for (String f : missingOnHost2) System.out.println("      " + f);
                }

                if (!missingOnHost1.isEmpty()) {
                    System.out.println("   ❌ Missing on " + host1 + ":");
                    for (String f : missingOnHost1) System.out.println("      " + f);
                }
            }
        }
    }

    private static List<String> listSubfolders(String user, String password, String host, String path) throws Exception {
        String cmd = "find " + path + " -mindepth 1 -maxdepth 1 -type d -exec basename {} \\;";
        String output = runCommand(user, password, host, cmd);
        return Arrays.asList(output.trim().split("\\s+"));
    }

    private static Set<String> getRelativeFiles(String user, String password, String host, String fullPath) throws Exception {
        String output = runCommand(user, password, host, "find " + fullPath + " -type f");
        return Arrays.stream(output.split("\n"))
                .map(f -> f.replaceFirst(fullPath + "/", ""))
                .collect(Collectors.toSet());
    }

    private static String runCommand(String user, String password, String host, String command) throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(user, host, 22);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        InputStream in = channel.getInputStream();
        channel.connect();

        Scanner scanner = new Scanner(in).useDelimiter("\\A");
        String result = scanner.hasNext() ? scanner.next() : "";

        channel.disconnect();
        session.disconnect();
        return result;
    }
}
