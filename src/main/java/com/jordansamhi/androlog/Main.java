package com.jordansamhi.androlog;

import com.jordansamhi.androlog.utils.Constants;
import com.jordansamhi.androspecter.SootUtils;
import com.jordansamhi.androspecter.TmpFolder;
import com.jordansamhi.androspecter.commandlineoptions.CommandLineOption;
import com.jordansamhi.androspecter.commandlineoptions.CommandLineOptions;
import com.jordansamhi.androspecter.instrumentation.Logger;
import com.jordansamhi.androspecter.printers.Writer;
import com.jordansamhi.androspecter.printers.Writer2;

import soot.options.Options;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Main {

    static void instrument(String apk_dir_path, String apk_name, String outputApk, int threads){

        System.out.println();
        Writer2 writer = new Writer2(outputApk, apk_name);

        boolean includeLibraries = false;
        String platforms = "/home/weiminn/Documents/AndroLog_custom/android-sdk";
        String logIdentifier = "COVAGENT_LOG";
        // String apk = "/home/weiminn/Documents/AndroLog_custom/MobileCoverage-TopGP-APKs/" + apkName;

        writer.pinfo("Instrumenting " + apk_name);
        writer.pinfo("Setting up environment...");
        SootUtils su = new SootUtils();
        su.setupSootWithOutput(platforms, Paths.get(apk_dir_path, apk_name).toString(), outputApk, true);
        Options.v().set_wrong_staticness(Options.wrong_staticness_ignore);
        applyThreadOption2(threads, writer);
        // System.out.print("Default number of threads: " + Options.v().num_threads());
        writer.psuccess("Done setting environment.");

        Path path = Paths.get(Paths.get(apk_dir_path, apk_name).toString());
        String fileName = path.getFileName().toString();

        // name of classes to be exclusively instrumented
        // probably not accurate for large scale apps
        // String packageName = "org.wikipedia";
        // Logger.v().setTargetPackage(packageName);

        Logger.Tuple methods_count = Logger.v().logAllMethods(logIdentifier, includeLibraries);
        writer.pinfo(methods_count.i1 + " total methods.");
        writer.pinfo(methods_count.i2 + " app methods to instrument.");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("mm:ss");
        try {
            long startTime = System.nanoTime();

            writer.pinfo("Start instrumentation.");
            Logger.v().instrument();
            // System.out.printf("%s v%s finished Instrumentation at %s\n%n", Constants.TOOL_NAME, Constants.VERSION, new Date());
            writer.psuccess("Done instrumentation.");

            long endTime = System.nanoTime();
            long durationNanos = endTime - startTime;
            long totalSeconds = durationNanos / 1_000_000_000L;
            long minutes = totalSeconds / 60;
            long seconds = totalSeconds % 60;
            String formattedDuration = String.format("%02d:%02d", minutes, seconds);
            writer.pinfo("Total time taken instrumenting: " + formattedDuration);
            writer.pinfo(
                "Time taken (in seconds) per method: " + 
                String.format(
                    "%.4f", 
                    ((double) totalSeconds) / ((double) methods_count.i2))
            );

            writer.pinfo("Exporting new apk...");
            Logger.v().exportNewApk(outputApk);
            writer.psuccess(String.format("Apk written in: %s", outputApk));

            writer.pinfo("Signing and aligning APK...");
            ApkPreparator ap = new ApkPreparator(String.format("%s/%s", outputApk, fileName), writer);
            ap.prepareApk();
            writer.psuccess("Done.");

            writer.pinfo("The apk is now instrumented, install it and execute it to generate logs.");
        } catch (Exception e){
            writer.perror("Exception while instrumenting");
            writer.perror(e.toString());

            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            writer.perror(sw.toString());
        }

    }

    public static void main(String[] args) {

        // change the string inside to solve compilation error about resolving types
        System.out.println("Starting...");

        // use lscpu commands to get number of available cores on the server

        String apks_dir_path = "/home/weiminn/Documents/AndroLog_custom/MobileCoverage-TopGP-APKs/";
        String instrumentation_output_dir = "/home/weiminn/Documents/AndroLog_custom/output_mtd_new";
        if (!Files.exists(Paths.get(instrumentation_output_dir))) {
            try {
                Files.createDirectories(Paths.get(instrumentation_output_dir));
            } catch (Exception e) {
                System.out.println("Cannot create output directory:");
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                System.out.println(sw.toString());
            }
        }

        // test sample
        // instrument(
        //     apks_dir_path, 
        //     "org.wikipedia_2.7.50447_apkmirror.com.apk",
        //     instrumentation_output_dir,
        //      0
        //     );

        List<String> to_skip = new ArrayList<>();
        to_skip.add("TikTok.apk"); // takes over 1 hour
        to_skip.add("AmazonPrimeVideo.apk"); // Need Update
        to_skip.add("Lensa_4.3.8+712_apkcombo.com.apk"); // Unable to launch
        to_skip.add("TheWeatherNetwork_7.18.1.8459.apk"); // Anti-root
        to_skip.add("SamsungSmartSwitchMobile.apk"); // No matching ABIs
        to_skip.add("REALTOR.caRealEstateHomes.apk"); // Older SDK
        to_skip.add("Pinterest.apk"); // DexPrinterException
        to_skip.add("CapCut-VideoEditor.apk"); // DexPrinterException
        to_skip.add("Instagram_280.0.0.18.114_apkcombo.com.apk"); // DexPrinterException
        // to_skip.add("PlutoTV_5.21.1_apkcombo.com.apk"); // can't connect to internet

        // only instrumentable with using only 1 thread
        List<String> single_thread = new ArrayList<>();
        single_thread.add("WhatsAppMessenger.apk"); // ConcurrentModificationException
        single_thread.add("SephoraBuyMakeupSkincare.apk"); // ConcurrentModificationException
        single_thread.add("VIZManga.apk"); // ConcurrentModificationException
        single_thread.add("LocalNewsBreakingLatest.apk"); // ConcurrentModificationException
        single_thread.add("Cardboard.apk"); // ConcurrentModificationException
        single_thread.add("SpotifyMusicandPodcasts.apk"); // ConcurrentModificationException, and problem aligning
        single_thread.add("Uber-Requestaride.apk"); // ConcurrentModificationException, and problem aligning
        single_thread.add("PictureThis-PlantIdentifier.apk"); // problem aligning
        single_thread.add("Shop_Allyourfavoritebrands_2.83.0_apkcombo.com.apk"); // problem aligning
        single_thread.add("AIMirrorAIArtPhotoEditor.apk"); // problem aligning
        single_thread.add("FIFAYourHomeforFootball_5.6.7_apkcombo.com.apk"); // problem aligning
        single_thread.add("MicrosoftTeams.apk"); // problem aligning

        File directory = new File(apks_dir_path);
        File[] listOfFiles = directory.listFiles();
        if(listOfFiles != null) {
            for (int i = 0; i < listOfFiles.length; i++) {

                String apk_name = listOfFiles[i].getName();

                if (Files.exists(Paths.get(instrumentation_output_dir, apk_name))) {
                    continue;
                }
                
                if (to_skip.contains(apk_name)) {
                    continue;
                }

                int threads = 90;
                if (single_thread.contains(apk_name)) {
                    threads = 1;
                }

                // System.out.println("File " + listOfFiles[i].getName());
                instrument(
                    apks_dir_path, 
                    apk_name,
                    instrumentation_output_dir, 
                    threads
                );
            }
        }

    }

    public static void main2(String[] args) {
        System.out.printf("%s v%s started on %s\n%n", Constants.TOOL_NAME, Constants.VERSION, new Date());

        CommandLineOptions options = CommandLineOptions.v();
        options.setAppName("AndroLog");
        options.addOption(new CommandLineOption("platforms", "p", "Platform file", true, true));
        options.addOption(new CommandLineOption("parse", "pa", "Parse log file", true, false));
        options.addOption(new CommandLineOption("parse-per-minute", "pam", "Parse log file per-minute", true, false));
        options.addOption(new CommandLineOption("output", "o", "Instrumented APK output", true, false));
        options.addOption(new CommandLineOption("json", "j", "Parsed logs JSON output", true, false));
        options.addOption(new CommandLineOption("apk", "a", "Apk file", true, true));
        options.addOption(new CommandLineOption("log-identifier", "l", "Log identifier", true, false));
        options.addOption(new CommandLineOption("classes", "c", "Log classes", false, false));
        options.addOption(new CommandLineOption("methods", "m", "Log methods", false, false));
        options.addOption(new CommandLineOption("statements", "s", "Log statements", false, false));
        options.addOption(new CommandLineOption("components", "cp", "Log Android components", false, false));
        options.addOption(new CommandLineOption("non-libraries", "n", "Whether to include libraries (by default: include libraries)", false, false));
        options.addOption(new CommandLineOption("package", "pkg", "Package name that will exclusively be instrumented", true, false));
        options.addOption(new CommandLineOption("method-calls", "mc", "Log method calls (e.g., a()-->b())", false, false));
        options.addOption(new CommandLineOption("threads", "t", "Number of threads to use in Soot", true, false));
        options.parseArgs(args);

        boolean includeLibraries = !CommandLineOptions.v().hasOption("n");

        String logIdentifier = Optional.ofNullable(options.getOptionValue("log-identifier")).orElse("ANDROLOG");
        String outputApk = Optional.ofNullable(options.getOptionValue("output")).orElse(TmpFolder.v().get());
        String outputJson = Optional.ofNullable(options.getOptionValue("json")).orElse(TmpFolder.v().get());

        Writer.v().pinfo("Setting up environment...");
        SootUtils su = new SootUtils();
        su.setupSootWithOutput(CommandLineOptions.v().getOptionValue("platforms"), CommandLineOptions.v().getOptionValue("apk"), outputApk, true);
        Options.v().set_wrong_staticness(Options.wrong_staticness_ignore);
        applyThreadOption();
        Writer.v().psuccess("Done.");

        Path path = Paths.get(CommandLineOptions.v().getOptionValue("apk"));
        String fileName = path.getFileName().toString();

        String packageName = null;
        if (CommandLineOptions.v().hasOption("pkg")) {
            packageName = CommandLineOptions.v().getOptionValue("package");
        }

        if (CommandLineOptions.v().hasOption("pa") || CommandLineOptions.v().hasOption("pam")) {
            Writer.v().pinfo("Generating Code Coverage Report...");
            String logFilePath = "";
            if (CommandLineOptions.v().hasOption("pa")) {
                logFilePath = CommandLineOptions.v().getOptionValue("parse");
            } else if (CommandLineOptions.v().hasOption("pam")) {
                logFilePath = CommandLineOptions.v().getOptionValue("parse-per-minute");
            }
            SummaryBuilder summaryBuilder = SummaryBuilder.v();
            summaryBuilder.setSootUtils(su);
            summaryBuilder.build(includeLibraries);

            LogParser lp = new LogParser(logIdentifier, summaryBuilder);
            lp.parseLogs(logFilePath);

            SummaryLogBuilder summaryLogBuilder = SummaryLogBuilder.v();

            SummaryStatistics stats = new SummaryStatistics();
            if (CommandLineOptions.v().hasOption("j")) {
                if (CommandLineOptions.v().hasOption("pa")) {
                    stats.compareSummariesToJson(summaryBuilder, summaryLogBuilder, outputJson);
                } else if (CommandLineOptions.v().hasOption("pam")) {
                    stats.compareSummariesPerMinuteToJson(summaryBuilder, summaryLogBuilder, outputJson);
                }
                Writer.v().psuccess("Done.");

                Writer.v().pinfo("The parsed logs are now available in " + outputJson);
            } else {
                if (CommandLineOptions.v().hasOption("pa")) {
                    stats.compareSummaries(summaryBuilder, summaryLogBuilder);
                } else if (CommandLineOptions.v().hasOption("pam")) {
                    stats.compareSummariesPerMinute(summaryBuilder, summaryLogBuilder);
                }
                Writer.v().psuccess("Done.");
            }
        } else {
            Writer.v().pinfo("Instrumentation in progress...");
            Logger.v().setTargetPackage(packageName);
            if (CommandLineOptions.v().hasOption("mc")) {
                Logger.v().logAllMethodCalls(logIdentifier, includeLibraries);
            }
            if (CommandLineOptions.v().hasOption("s")) {
                Logger.v().logAllStatements(logIdentifier, includeLibraries);
            }
            if (CommandLineOptions.v().hasOption("m")) {
                Logger.v().logAllMethods(logIdentifier, includeLibraries);
            }
            if (CommandLineOptions.v().hasOption("c")) {
                Logger.v().logAllClasses(logIdentifier, includeLibraries);
            }
            if (CommandLineOptions.v().hasOption("cp")) {
                Logger.v().logActivities(logIdentifier, includeLibraries);
                Logger.v().logContentProviders(logIdentifier, includeLibraries);
                Logger.v().logServices(logIdentifier, includeLibraries);
                Logger.v().logBroadcastReceivers(logIdentifier, includeLibraries);
            }
            Logger.v().instrument();
            System.out.printf("%s v%s finished Instrumentation at %s\n%n", Constants.TOOL_NAME, Constants.VERSION, new Date());
            Writer.v().psuccess("Done instrumentation.");
            Writer.v().pinfo("Exporting new apk...");
            Logger.v().exportNewApk(outputApk);
            Writer.v().psuccess(String.format("Apk written in: %s", outputApk));

            Writer.v().pinfo("Signing and aligning APK...");
            // ApkPreparator ap = new ApkPreparator(String.format("%s/%s", outputApk, fileName));
            // ap.prepareApk();
            // Writer.v().psuccess("Done.");

            // Writer.v().pinfo("The apk is now instrumented, install it and execute it to generate logs.");
        }
    }

    /**
     * Configures the number of threads used by the Soot framework based on the
     * command-line option provided by the user.
     * <p>
     * If the "--threads" (or "-t") option is specified and contains a valid positive
     * integer, this value is passed to {@code Options.v().set_num_threads()}.
     * Otherwise, Soot's default thread configuration is used.
     * <p>
     * Logs appropriate messages for valid, missing, or invalid input.
     * Exits the program if the input value is non-numeric or non-positive.
     */
    private static void applyThreadOption() {
        if (CommandLineOptions.v().hasOption("threads")) {
            try {
                int numThreads = Integer.parseInt(CommandLineOptions.v().getOptionValue("threads"));
                if (numThreads > 0) {
                    Options.v().set_num_threads(numThreads);
                    Writer.v().pinfo(String.format("Using %d threads for Soot processing", numThreads));
                } else {
                    Writer.v().perror("Invalid number of threads. Must be a positive integer.");
                    System.exit(1);
                }
            } catch (NumberFormatException e) {
                Writer.v().perror("Invalid format for thread count. Please provide a numeric value.");
                System.exit(1);
            }
        } else {
            Writer.v().pinfo("No thread count specified. Using Soot's default thread configuration.");
        }
    }

    private static void applyThreadOption2(int threads, Writer2 writer) {
        if (threads > 0) {
            try {
                Options.v().set_num_threads(threads);
                writer.pinfo(String.format("Using %d threads for Soot processing", threads));
            } catch (NumberFormatException e) {
                writer.perror("Invalid format for thread count. Please provide a numeric value.");
                System.exit(1);
            }
        } else {
            writer.pinfo("No thread count specified. Using Soot's default thread configuration.");
        }
    }
}
