/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.blinkt.openvpn.R;

public class OpenVPNThread implements Runnable {
    private static final String DUMP_PATH_STRING = "Dump path: ";
    @SuppressLint("SdCardPath")
    private static final String BROKEN_PIE_SUPPORT = "/data/data/de.blinkt.openvpn/cache/pievpn";
    private final static String BROKEN_PIE_SUPPORT2 = "syntax error";
    private static final String TAG = "OpenVPN";
    // 1380308330.240114 18000002 Send to HTTP proxy: 'X-Online-Host: bla.blabla.com'
    private static final Pattern LOG_PATTERN = Pattern.compile("(\\d+).(\\d+) ([0-9a-f])+ (.*)");
    public static final int M_FATAL = (1 << 4);
    public static final int M_NONFATAL = (1 << 5);
    public static final int M_WARN = (1 << 6);
    public static final int M_DEBUG = (1 << 7);
    private String[] mArgv;
    private static Process mProcess;
    private String mNativeDir;
    private String mTmpDir;
    private static OpenVPNService mService;
    private String mDumpPath;
    private boolean mBrokenPie = false;
    private boolean mNoProcessExitStatus = false;
    private static String mExtractedExecPath = null;


    public OpenVPNThread(OpenVPNService service, String[] argv, String nativelibdir, String tmpdir) {
        mArgv = argv;
        mNativeDir = nativelibdir;
        mTmpDir = tmpdir;
        mService = service;
    }

    public OpenVPNThread() {
    }

    public void stopProcess() {

        if (mProcess != null) {
        mProcess.destroy();
            mProcess = null;   // IMPORTANT
        }
        else {
            VpnStatus.logError("OpenVPN process is null, cannot destroy");
        }

    }

    void setReplaceConnection()
    {
        mNoProcessExitStatus=true;
    }

    @Override
    public void run() {
        try {
            Log.i(TAG, "Starting openvpn");
            startOpenVPNThreadArgs(mArgv);
            Log.i(TAG, "OpenVPN process exited");
        } catch (Exception e) {
            VpnStatus.logException("Starting OpenVPN Thread", e);
            Log.e(TAG, "OpenVPNThread Got " + e.toString());
        } finally {
            int exitvalue = 0;
            try {
                if (mProcess != null)
                    exitvalue = mProcess.waitFor();
                mProcess = null;

            } catch (IllegalThreadStateException ite) {
                VpnStatus.logError("Illegal Thread state: " + ite.getLocalizedMessage());
            } catch (InterruptedException ie) {
                VpnStatus.logError("InterruptedException: " + ie.getLocalizedMessage());
            }
            if (exitvalue != 0) {
                VpnStatus.logError("Process exited with exit value " + exitvalue);
                if (mBrokenPie) {
                    /* This will probably fail since the NoPIE binary is probably not written */
                    String[] noPieArgv = VPNLaunchHelper.replacePieWithNoPie(mArgv);

                    // We are already noPIE, nothing to gain
                    if (!noPieArgv.equals(mArgv)) {
                        mArgv = noPieArgv;
                        VpnStatus.logInfo("PIE Version could not be executed. Trying no PIE version");
                        run();
                    }

                }

            }

            if (!mNoProcessExitStatus)
                VpnStatus.updateStateString("NOPROCESS", "No process running.", R.string.state_noprocess, ConnectionStatus.LEVEL_NOTCONNECTED);

            if (mDumpPath != null) {
                try {
                    BufferedWriter logout = new BufferedWriter(new FileWriter(mDumpPath + ".log"));
                    SimpleDateFormat timeformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMAN);
                    for (LogItem li : VpnStatus.getlogbuffer()) {
                        String time = timeformat.format(new Date(li.getLogtime()));
                        logout.write(time + " " + li.getString(mService) + "\n");
                    }
                    logout.close();
                    VpnStatus.logError(R.string.minidump_generated);
                } catch (IOException e) {
                    VpnStatus.logError("Writing minidump log: " + e.getLocalizedMessage());
                }
            }

            if (!mNoProcessExitStatus)
                mService.openvpnStopped();
            Log.i(TAG, "Exiting");
            mProcess = null;
        }
    }

    public static boolean stop() {
        mService.openvpnStopped();

        if (mProcess != null) {
            try {
                mProcess.destroy();
            } catch (Exception ignored) {}

            mProcess = null;
        } else {
            Log.e(TAG, "stop() called but mProcess is NULL");
        }
        if (mExtractedExecPath != null) {
            try {
                new java.io.File(mExtractedExecPath).delete();
            } catch (Throwable ignored) {}
            mExtractedExecPath = null;
        }

        return true;
    }



    private void startOpenVPNThreadArgs(String[] argv) {
        LinkedList<String> argvlist = new LinkedList<String>();

        Collections.addAll(argvlist, argv);
// --- Add this right after Collections.addAll(argvlist, argv);
        try {
            // ensure the first element is an executable file; if it points to a .so in APK, extract it
            if (argvlist.size() > 0) {
                String original0 = argvlist.get(0);
                String execPath = ensureExecutableFromSo(original0);
                if (execPath != null && !execPath.equals(original0)) {
                    argvlist.set(0, execPath);
                    Log.i(TAG, "Replaced argv[0] with extracted executable: " + execPath);
                } else {
                    Log.i(TAG, "argv[0] unchanged: " + original0);
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to ensure executable for argv[0]: " + t.getMessage());
        }

        ProcessBuilder pb = new ProcessBuilder(argvlist);
        // Hack O rama

        String lbpath = genLibraryPath(argv, pb);

        pb.environment().put("LD_LIBRARY_PATH", lbpath);
        pb.environment().put("TMPDIR", mTmpDir);

        pb.redirectErrorStream(true);
        try {

                // Diagnostic: log command and binary path for debugging
                StringBuilder cmdSb = new StringBuilder();
                for (String s : argvlist) {
                    cmdSb.append(s).append(" ");
                }
                Log.i(TAG, "Attempting to start OpenVPN with command: " + cmdSb.toString());

                // Inspect argv[0] (binary path) before start
                try {
                    String binPath = argvlist.size() > 0 ? argvlist.get(0) : null;
                    if (binPath != null) {
                        Log.i(TAG, "OpenVPN binary path (argv[0]) = " + binPath);
                        try {
                            java.io.File binFile = new java.io.File(binPath);
                            if (!binFile.exists()) {
                                Log.e(TAG, "OpenVPN binary does not exist: " + binPath);
                            } else {
                                if (!binFile.canExecute()) {
                                    Log.e(TAG, "OpenVPN binary exists but is not executable: " + binPath);
                                } else {
                                    Log.i(TAG, "OpenVPN binary exists and is executable: " + binPath);
                                }
                            }
                        } catch (Exception ex) {
                            Log.w(TAG, "Could not stat binary: " + ex.getMessage());
                        }
                    } else {
                        Log.w(TAG, "argv[0] (binary path) is null");
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "Binary path check failed: " + t.getMessage());
                }

                // Try starting process
                try {
                    mProcess = pb.start();
                    Log.i(TAG, "pb.start() succeeded, process object obtained");
                } catch (Exception e) {
                    Log.e(TAG, "Failed to start OpenVPN process (pb.start() threw): " + e.getMessage(), e);
                    mProcess = null;
                }

                // SAFETY: if process didn't start, return so we don't dereference null
                if (mProcess == null) {
                    VpnStatus.logError("OpenVPN process not started! Aborting startOpenVPNThreadArgs()");
                    return;
                }

                // Close the output, since we don't need it
                try {
                    mProcess.getOutputStream().close();
                } catch (IOException ioe) {
                    Log.w(TAG, "Unable to close process output stream", ioe);
                }

                InputStream in = mProcess.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(in));


                while (true) {
                String logline = br.readLine();
                if (logline == null)
                    return;

                if (logline.startsWith(DUMP_PATH_STRING))
                    mDumpPath = logline.substring(DUMP_PATH_STRING.length());

                if (logline.startsWith(BROKEN_PIE_SUPPORT) || logline.contains(BROKEN_PIE_SUPPORT2))
                    mBrokenPie = true;

                Matcher m = LOG_PATTERN.matcher(logline);
                int logerror = 0;
                if (m.matches()) {
                    int flags = Integer.parseInt(m.group(3), 16);
                    String msg = m.group(4);
                    int logLevel = flags & 0x0F;

                    VpnStatus.LogLevel logStatus = VpnStatus.LogLevel.INFO;

                    if ((flags & M_FATAL) != 0)
                        logStatus = VpnStatus.LogLevel.ERROR;
                    else if ((flags & M_NONFATAL) != 0)
                        logStatus = VpnStatus.LogLevel.WARNING;
                    else if ((flags & M_WARN) != 0)
                        logStatus = VpnStatus.LogLevel.WARNING;
                    else if ((flags & M_DEBUG) != 0)
                        logStatus = VpnStatus.LogLevel.VERBOSE;

                    if (msg.startsWith("MANAGEMENT: CMD"))
                        logLevel = Math.max(4, logLevel);

                    if ((msg.endsWith("md too weak") && msg.startsWith("OpenSSL: error")) || msg.contains("error:140AB18E"))
                        logerror = 1;

                    VpnStatus.logMessageOpenVPN(logStatus, logLevel, msg);
                    if (logerror==1)
                        VpnStatus.logError("OpenSSL reported a certificate with a weak hash, please the in app FAQ about weak hashes");

                } else {
                    VpnStatus.logInfo("P:" + logline);
                }

                if (Thread.interrupted()) {
                    throw new InterruptedException("OpenVpn process was killed form java code");
                }
            }
        } catch (InterruptedException | IOException e) {
            Log.e(TAG, "Failed to start or read from OpenVPN process", e);
            VpnStatus.logException("Error reading from output of OpenVPN process", e);
            // ensure mProcess cleaned up
            stopProcess();
            // make sure mProcess null so other code knows it did not start
            mProcess = null;
        }


    }
    /**
     * If argv[0] points to a .so inside the APK's lib/ folder, copy it to app cache,
     * chmod it executable and return the new path. If the file is already a regular
     * executable file, return it unchanged.
     */
//    private String ensureExecutableFromSo(String candidatePath) {
//        try {
//            if (candidatePath == null) return candidatePath;
//
//            // Heuristic: .so in a lib/ folder inside apk/native dir -> copy out
//            if (!candidatePath.endsWith(".so")) {
//                // not a .so: assume it's already an executable path
//                return candidatePath;
//            }
//
//            if (mService == null) {
//                Log.w(TAG, "ensureExecutableFromSo: mService is null, can't extract");
//                return candidatePath;
//            }
//
//            java.io.File srcFile = new java.io.File(candidatePath);
//            if (!srcFile.exists()) {
//                Log.e(TAG, "ensureExecutableFromSo: source .so does not exist: " + candidatePath);
//                return candidatePath;
//            }
//
//            // Destination in cache with timestamp to avoid collisions
//            java.io.File cacheDir = mService.getCacheDir();
//            if (cacheDir == null) cacheDir = mService.getFilesDir();
//            String outName = "ovpnexec-" + System.currentTimeMillis();
//            java.io.File outFile = new java.io.File(cacheDir, outName);
//
//            // Copy bytes
//            try (java.io.InputStream is = new java.io.FileInputStream(srcFile);
//                 java.io.OutputStream os = new java.io.FileOutputStream(outFile)) {
//                byte[] buf = new byte[4096];
//                int r;
//                while ((r = is.read(buf)) != -1) os.write(buf, 0, r);
//                os.flush();
//            } catch (Exception e) {
//                Log.e(TAG, "ensureExecutableFromSo: failed to copy .so to cache: " + e.getMessage(), e);
//                return candidatePath;
//            }
//
//            // Make executable: try Java API, then fallback to chmod shell
//            boolean execOk = outFile.setExecutable(true, true);
//            if (!execOk) {
//                try {
//                    Process chmod = Runtime.getRuntime().exec(new String[] { "chmod", "700", outFile.getAbsolutePath() });
//                    int rc = chmod.waitFor();
//                    Log.i(TAG, "chmod rc=" + rc + " for " + outFile.getAbsolutePath());
//                    execOk = outFile.canExecute();
//                } catch (Throwable t) {
//                    Log.w(TAG, "ensureExecutableFromSo: chmod failed: " + t.getMessage());
//                }
//            }
//
//            if (!outFile.canExecute()) {
//                Log.e(TAG, "ensureExecutableFromSo: copied file is not executable: " + outFile.getAbsolutePath());
//                // optional: delete broken copy
//                // outFile.delete();
//                return candidatePath;
//            }
//
//            Log.i(TAG, "ensureExecutableFromSo: extracted executable to " + outFile.getAbsolutePath());
//            return outFile.getAbsolutePath();
//
//        } catch (Throwable t) {
//            Log.e(TAG, "ensureExecutableFromSo: unexpected error: " + t.getMessage(), t);
//            return candidatePath;
//        }
//    }
    /**
     * If argv[0] points to a .so inside the APK's lib/ folder, copy it to app cache,
     * chmod it executable and return the new path. If the file is already a regular
     * executable file on disk, return it unchanged. Skips copy if source exists and is executable.
     */
    private String ensureExecutableFromSo(String candidatePath) {
        try {
            if (candidatePath == null) return candidatePath;

            // Quick check: If it exists and is executable, use as-is (e.g., extracted to /lib/arm64/)
            File srcFile = new File(candidatePath);
            if (srcFile.exists() && srcFile.canExecute()) {
                Log.i(TAG, "ensureExecutableFromSo: Source already exists and executable: " + candidatePath);
                return candidatePath;  // Use the on-disk version directly
            }

            // Heuristic: .so in a lib/ folder inside apk/native dir -> copy out (but only if not already good)
            if (!candidatePath.endsWith(".so")) {
                // not a .so: assume it's already an executable path
                return candidatePath;
            }

            if (mService == null) {
                Log.w(TAG, "ensureExecutableFromSo: mService is null, can't extract");
                return candidatePath;
            }

            if (!srcFile.exists()) {
                Log.e(TAG, "ensureExecutableFromSo: source .so does not exist: " + candidatePath);
                return candidatePath;
            }

            // Ensure source is executable before copy (fallback chmod)
            if (!srcFile.canExecute()) {
                Log.w(TAG, "ensureExecutableFromSo: Source not executable, attempting chmod");
                try {
                    Process chmod = Runtime.getRuntime().exec(new String[] { "chmod", "700", srcFile.getAbsolutePath() });
                    int rc = chmod.waitFor();
                    Log.i(TAG, "chmod on source rc=" + rc + " for " + srcFile.getAbsolutePath());
                    if (srcFile.canExecute()) {
                        Log.i(TAG, "ensureExecutableFromSo: Chmod succeeded on source: " + srcFile.getAbsolutePath());
                        return srcFile.getAbsolutePath();  // Now use source directly
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "ensureExecutableFromSo: chmod on source failed: " + t.getMessage());
                }
            }

            // Destination in cache with timestamp to avoid collisions
            File cacheDir = mService.getCacheDir();
            if (cacheDir == null) cacheDir = mService.getFilesDir();
            String outName = "ovpnexec-" + System.currentTimeMillis();
            File outFile = new File(cacheDir, outName);

            // Copy bytes
            try (InputStream is = new FileInputStream(srcFile);
                 OutputStream os = new FileOutputStream(outFile)) {
                byte[] buf = new byte[4096];
                int r;
                while ((r = is.read(buf)) != -1) os.write(buf, 0, r);
                os.flush();
            } catch (Exception e) {
                Log.e(TAG, "ensureExecutableFromSo: failed to copy .so to cache: " + e.getMessage(), e);
                return candidatePath;
            }

            // Make executable: try Java API, then fallback to chmod shell
            boolean execOk = outFile.setExecutable(true, true);
            if (!execOk) {
                try {
                    Process chmod = Runtime.getRuntime().exec(new String[] { "chmod", "700", outFile.getAbsolutePath() });
                    int rc = chmod.waitFor();
                    Log.i(TAG, "chmod rc=" + rc + " for " + outFile.getAbsolutePath());
                    execOk = outFile.canExecute();
                } catch (Throwable t) {
                    Log.w(TAG, "ensureExecutableFromSo: chmod failed: " + t.getMessage());
                }
            }

            if (!outFile.canExecute()) {
                Log.e(TAG, "ensureExecutableFromSo: copied file is not executable: " + outFile.getAbsolutePath());
                // optional: delete broken copy
                // outFile.delete();
                return candidatePath;
            }

            Log.i(TAG, "ensureExecutableFromSo: extracted executable to " + outFile.getAbsolutePath());
            return outFile.getAbsolutePath();

        } catch (Throwable t) {
            Log.e(TAG, "ensureExecutableFromSo: unexpected error: " + t.getMessage(), t);
            return candidatePath;
        }
    }
//    private String genLibraryPath(String[] argv, ProcessBuilder pb) {
//       // Detect the ABI exactly as packaged inside the APK
//        // Example values: arm64-v8a, armeabi-v7a, x86_64
//        String abi = android.os.Build.SUPPORTED_ABIS[0];
//        Log.e("genlibrary", "SUPPORTED_ABIS = " + Arrays.toString(Build.SUPPORTED_ABIS));
//        if (abi.equals("arm64")) {abi = "arm64-v8a";}
//        // Map other possible weird cases
//        if (abi.equals("armeabi")) {
//            abi = "armeabi-v7a";
//        }
//        // Hack until I find a good way to get the real library path
//        String applibbase = argv[0].replaceFirst("/cache/.*$", "/lib");
//        Log.e("VPN", "applibbase = " + applibbase);
////        String applibpath = applibbase ;
////        Log.e("VPN", "applibpath = " + applibpath + ", abi = " + abi);
//        String lbpath = pb.environment().get("LD_LIBRARY_PATH");
//        Log.e("VPN", "original LD_LIBRARY_PATH = " + lbpath);
//        if (lbpath == null){
//            lbpath = applibbase;
//        Log.e("VPN", "LD_LIBRARY_PATH was null, now set to applibpath: " + lbpath);}
//        else {
//            lbpath = applibbase + ":" + lbpath;
//            Log.e("VPN", "LD_LIBRARY_PATH updated with applibpath: " + lbpath);
//        }
//        if (!applibbase.equals(mNativeDir)) {
//            lbpath = mNativeDir + ":" + lbpath;
//            Log.e("VPN", "Added mNativeDir, final LD_LIBRARY_PATH: " + lbpath);
//        }else {
//            Log.e("VPN", "mNativeDir equals applibpath, LD_LIBRARY_PATH unchanged");
//        }
//        return lbpath;
//    }
private String genLibraryPath(String[] argv, ProcessBuilder pb) {
    // Use mNativeDir directly (passed from caller, e.g., /data/app/.../lib/arm64)
    String applibbase = mNativeDir;  // Dir only, no file
    Log.e("VPN", "applibbase = " + applibbase);

    String lbpath = pb.environment().get("LD_LIBRARY_PATH");
    Log.e("VPN", "original LD_LIBRARY_PATH = " + lbpath);
    if (lbpath == null) {
        lbpath = applibbase;  // Dir path
        Log.e("VPN", "LD_LIBRARY_PATH was null, now set to applibpath: " + lbpath);
    } else {
        lbpath = applibbase + ":" + lbpath;
        Log.e("VPN", "LD_LIBRARY_PATH updated with applibpath: " + lbpath);
    }
    // No need for the "Added mNativeDir" check—applibbase is already mNativeDir

    return lbpath;
}
}
