package com.example.app.views;

import com.example.app.data.entity.FTPFile;
import com.example.app.utils.TaskStatus;
import com.example.app.utils.Util;
import com.jcraft.jsch.*;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

/**
 * A simple SFTP client using JSCH http://www.jcraft.com/jsch/
 */
public final class SftpClient {
    private final String      host;
    private final int         port;
    private final String      username;
    private final JSch jsch;
    private ChannelSftp channel;
    private       Session     session;
    private static final Logger logger = LoggerFactory.getLogger(SftpClient.class);

    /**
     * @param host     remote host
     * @param port     remote port
     * @param username remote username
     */
    public SftpClient(String host, int port, String username) {
        logger.info("SftpClient(): host ="+host+" port = "+port+ " username = "+username);
        this.host     = host;
        this.port     = port;
        this.username = username;
        jsch          = new JSch();
    }

    /**
     * Use default port 22
     *
     * @param host     remote host
     * @param username username on host
     */
    public SftpClient(String host, String username) {
        this(host, 22, username);
    }

    /**
     * Authenticate with remote using password
     *
     * @param password password of remote
     * @throws JSchException If there is problem with credentials or connection
     */
    public void authPassword(String password) throws JSchException {
        session = jsch.getSession(username, host, port);
        //disable known hosts checking
        //if you want to set knows hosts file You can set with jsch.setKnownHosts("path to known hosts file");
        var config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.setPassword(password);
        session.connect();
        channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();
    }


    public void authKey(String key, String pass) throws JSchException {
        logger.trace("authKey(): key = "+key +" password = "+pass);
        try {
            byte[] privateKey = key.getBytes();
            jsch.addIdentity("identity_name", privateKey, null, pass != null ? pass.getBytes() : null);
            //  jsch.addIdentity(key, pass);
            //jsch.addIdentity(keyPath, pass);
            session = jsch.getSession(username, host, port);
            //disable known hosts checking
            //if you want to set knows hosts file You can set with jsch.setKnownHosts("path to known hosts file");
            var config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            //   session.setTimeout(6000);
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();
            logger.info("authKey(): chanel connect");
        } catch (JSchException e) {
            logger.error("authKey(): Error during authentication or connection: " + e.getMessage());
            Notification.show("Error during authentication or connection: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        } catch (Exception e) {
            logger.error("authKey(): An unexpected error occurred: " + e.getMessage());
            Notification.show("An unexpected error occurred: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    /**
     * List all files including directories
     *
     * @param remoteDir Directory on remote from which files will be listed
     * @throws SftpException If there is any problem with listing files related to permissions etc
     * @throws JSchException If there is any problem with connection
     */
    @SuppressWarnings("unchecked")
    public void listFiles(String remoteDir) throws SftpException, JSchException {
        if (channel == null) {
            throw new IllegalArgumentException("Connection is not available");
        }
     //   System.out.printf("Listing [%s]...%n", remoteDir);
        channel.cd(remoteDir);
        Vector<ChannelSftp.LsEntry> files = channel.ls(".");
        for (ChannelSftp.LsEntry file : files) {
            var name        = file.getFilename();
            var attrs       = file.getAttrs();
            var permissions = attrs.getPermissionsString();
            var size        = Util.humanReadableByteCount(attrs.getSize());
            if (attrs.isDir()) {
                size = "PRE";
            }
            System.out.printf("[%s] %s(%s) Atime: %s %n", permissions, name, size, localTimeUtc(Instant.ofEpochSecond(attrs.getMTime())));
        }
    }


    public List<FTPFile> getFiles(String remoteDir, Long fromDate, Long toDate) throws SftpException, JSchException {
        logger.info("getFiles(): Listing [%s]...%n", remoteDir);
        if (channel == null) {
            throw new IllegalArgumentException("Connection is not available");
        }
     //   System.out.printf("Listing [%s]...%n", remoteDir);
        channel.cd(remoteDir);

        List<FTPFile>_files=new ArrayList<FTPFile>();

        Vector<ChannelSftp.LsEntry> files = channel.ls(".");
        for (ChannelSftp.LsEntry file : files) {
            FTPFile f = new FTPFile();


            var name        = file.getFilename();
            var attrs       = file.getAttrs();
            var permissions = attrs.getPermissionsString();
            var size        = Util.humanReadableByteCount(attrs.getSize());

            f.setSize(attrs.getSize());
            f.setName(file.getFilename());
            f.setErstellungszeit(localTimeUtc(Instant.ofEpochSecond(attrs.getMTime())));

            if (attrs.isDir()) {
                size = "<DIR>";
            }
        //    System.out.printf("[%s] %s(%s) Atime: %s %n", permissions, name, size, localTimeUtc(Instant.ofEpochSecond(attrs.getMTime())));

            if (attrs.getMTime() >= fromDate && attrs.getMTime()<= toDate) {
                _files.add(f);
            }
        }

        return _files;

    }

    public static LocalDateTime localTimeUtc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.systemDefault());
    }

    /**
     * Upload a file to remote
     *
     * @param localPath  full path of location file
     * @param remotePath full path of remote file
     * @throws JSchException If there is any problem with connection
     * @throws SftpException If there is any problem with uploading file permissions etc
     */
    public void uploadFile(String localPath, String remotePath) throws JSchException, SftpException {
        System.out.printf("Uploading [%s] to [%s]...%n", localPath, remotePath);
        if (channel == null) {
            throw new IllegalArgumentException("Connection is not available");
        }
        channel.put(localPath, remotePath);
    }

    /**
     * Download a file from remote
     *
     * @param remotePath full path of remote file
     * @param localPath  full path of where to save file locally
     * @throws SftpException If there is any problem with downloading file related permissions etc
     */
    public void downloadFile(String remotePath, String localPath) throws SftpException {
        logger.info("downloadFile(): Downloading [%s] to [%s]...%n", remotePath, localPath);
     //   System.out.printf("Downloading [%s] to [%s]...%n", remotePath, localPath);
        if (channel == null) {
            throw new IllegalArgumentException("Connection is not available");
        }
        channel.get(remotePath, localPath);
    }

    /**
     * Delete a file on remote
     *
     * @param remoteFile full path of remote file
     * @throws SftpException If there is any problem with deleting file related to permissions etc
     */
    public void delete(String remoteFile) throws SftpException {
        logger.info("downloadFile(): Deleting [%s]...%n", remoteFile);
     //   System.out.printf("Deleting [%s]...%n", remoteFile);
        if (channel == null) {
            throw new IllegalArgumentException("Connection is not available");
        }
        channel.rm(remoteFile);
    }


    public byte[] readFile(String fileName) throws SftpException, IOException {
        logger.info("readFile(): get Bytes from File [%s]%n", fileName);
     //   System.out.printf("get Bytes from File [%s]%n", fileName);
        if (channel == null) {
            throw new IllegalArgumentException("Connection is not available");
        }
        InputStream inputStream = channel.get(fileName);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            var fileData = outputStream.toByteArray();

            outputStream.close();
            inputStream.close();
            channel.disconnect();
            session.disconnect();

        return fileData;
    }


    public void TailRemoteLogFile(StringBuilder logTextArea, String FileName, TaskStatus stat, TextArea tailTextArea) throws JSchException, IOException, SftpException {
        logger.info("TailRemoteLogFile(): tail -f file = " +FileName);
        VaadinSession vaadinSession = VaadinSession.getCurrent();
        VaadinService vaadinService = VaadinService.getCurrent();
        UI ui = UI.getCurrent();
        clearLog(logTextArea);
        if (channel == null) {
            throw new IllegalArgumentException("Connection is not available");
        }

        ChannelSftp sftpChannel = (ChannelSftp) channel;


        String command = "tail -f " +FileName;
      //  String command = "tail -300 " +FileName;
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);


    new Thread(() -> {
        Thread.currentThread().setName("Tail-Thread");
        VaadinService.setCurrent(vaadinService);
        VaadinSession.setCurrent(vaadinSession);
        UI.setCurrent(ui);

    try {
        channel.connect();
    } catch (JSchException e) {
        throw new RuntimeException(e);
    }

    // Read the output of the tail command and display it
    BufferedReader reader = null;
    try {
        reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
        String line="";
        while (!Thread.currentThread().isInterrupted()) {
            try {

                line = reader.readLine();
              //  if (line.startsWith("stop")) break;
                if (!stat.isActive()) break;
                if (line==null) break;
            } catch (IOException e) {
                //throw new RuntimeException(e);
              //  System.out.println("readLine-Abfrage in SftpClient unterbrochen " + e.getMessage() );
                logger.info("readLine-Abfrage in SftpClient unterbrochen " + e.getMessage() );
                sftpChannel.exit();
                session.disconnect();
            }
            //  System.out.println(line);
            updateLog(line,logTextArea, tailTextArea);
           // generateLogEntries(logTextArea);
//            UI.getCurrent().access(() -> {
//                tailTextArea.setValue(logTextArea.toString());  // Update the UI with new log content
//            });
        }

        sftpChannel.exit();
        session.disconnect();

    }).start();

    }

  /*  public void ReadRemoteLogFile(UI ui, TextArea logTextArea, String FileName, TaskStatus stat  ) throws JSchException, IOException, SftpException {

        if (channel == null) {
            throw new IllegalArgumentException("Connection is not available");
        }

       // ChannelSftp sftpChannel = (ChannelSftp) channel;


        //String command = "tail -f " +FileName;
        String command = "tail -300 " +FileName;
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);



            try {
                channel.connect();
            } catch (JSchException e) {
                throw new RuntimeException(e);
            }

            // Read the output of the tail command and display it
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String line;
            while (true) {
                try {
                    line = reader.readLine();
                    if (line==null) break;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                //  System.out.println(line);
                updateLog(line,logTextArea);
            }

         //   sftpChannel.exit();
            session.disconnect();

//        logTextArea.getElement().executeJs("this.scrollTop = this.scrollHeight");


    }*/


    public void ReadRemoteLogFile(UI ui, StringBuilder logTextArea, String FileName, TextArea tailTextArea) throws JSchException, IOException, SftpException {
        logger.info("ReadRemoteLogFile(): head -300 file = " +FileName);
        if (channel == null) {
            throw new IllegalArgumentException("Connection is not available");
        }

        // ChannelSftp sftpChannel = (ChannelSftp) channel;


        //String command = "tail -f " +FileName;
        String command = "head -300 " +FileName;
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);



        try {
            channel.connect();
        } catch (JSchException e) {
            throw new RuntimeException(e);
        }

        // Read the output of the tail command and display it
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String line;

        clearLog(logTextArea);

        while (true) {
            try {
                line = reader.readLine();
                if (line==null) break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            //  System.out.println(line);
            updateLog(line,logTextArea, tailTextArea);
        }

        //####################

       // command = "head 300 " +FileName;
        command = "tail -300 " +FileName;

        channel.disconnect();
        channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);



        try {
            channel.connect();
        } catch (JSchException e) {
            throw new RuntimeException(e);
        }

        // Read the output of the tail command and display it
        reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    /*
        updateLog("###################### TAIL -300 ##############################",logTextArea);
        updateLog("###################### TAIL -300 ##############################",logTextArea, tailTextArea);
        while (true) {
            try {
                line = reader.readLine();
                if (line==null) break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            //  System.out.println(line);
            updateLog(line,logTextArea, tailTextArea);
        }
*/


        //   sftpChannel.exit();
        session.disconnect();



//        logTextArea.getElement().executeJs("this.scrollTop = this.scrollHeight");


    }

    // Simulated method to generate new log entries every 2 seconds
//    private void generateLogEntries(StringBuilder tailTextArea) {
//        Timer timer = new Timer();
//        timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                // Simulate a new log entry
//                String newLogLine = "Log entry at " + LocalTime.now();
//
//                // Add the new log line and scroll automatically
//                updateLog(newLogLine, tailTextArea, tailTextArea);
//            }
//        }, 0, 2000); // Repeat every 2 seconds
//    }

    private void updateLog(String line, StringBuilder tailTextAreaContent, TextArea tailTextArea) {
        logger.info("updateLog(): update log textarea " );
        if (tailTextArea.isEmpty()) {
            tailTextAreaContent.append("").append(line);
        } else {
            tailTextAreaContent.append("\n").append(line);
        }
        UI.getCurrent().access(() -> {
            tailTextArea.setValue(tailTextAreaContent.toString());  // Update the UI with new log content
            tailTextArea.getElement().executeJs("this._inputField.scrollTop = this._inputField.scrollHeight - this._inputField.clientHeight;");
        });

        // Scroll to the bottom of the TextArea after the new content is added
//        VaadinSession.getCurrent().lock();
//        tailTextArea.getElement().executeJs("this.inputElement.scrollTop = this.inputElement.scrollHeight;");
//        VaadinSession.getCurrent().unlock();
//        VaadinSession.getCurrent().lock();
//
//        tailTextArea.getElement().executeJs(
//                "this.inputElement.value += $0; this._updateHeight(); this._inputField.scrollTop = this._inputField.scrollHeight - this._inputField.clientHeight;",
//                "\n" + line
//        );
//        VaadinSession.getCurrent().unlock();
    }

    private void clearLog(StringBuilder tailTextArea) {
        logger.info("clearLog(): clear log area " );
        tailTextArea.setLength(0);
//        VaadinSession.getCurrent().lock();
//
//        tailTextArea.getElement().executeJs(
//                "this.inputElement.value = $0; this._updateHeight(); this._inputField.scrollTop = this._inputField.scrollHeight - this._inputField.clientHeight;",
//                ""
//        );
//        VaadinSession.getCurrent().unlock();
    }




    /**
     * Disconnect from remote
     */
    public void close() {
        if (channel != null) {
            channel.exit();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    public void executeCommand(String directory, String command, TextArea logTextArea) throws Exception {
        if (session == null || !session.isConnected()) {
            throw new IllegalStateException("Session is not connected");
        }

        // Construct the full command
        String fullCommand = "cd " + directory + " && " + directory +"/"+command;
        logger.info("Executing command: " + fullCommand);

        ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
        channelExec.setCommand(fullCommand);

        InputStream inputStream = channelExec.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        InputStream errorStream = channelExec.getErrStream();
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream));

        channelExec.connect();

        String line;
        StringBuilder output = new StringBuilder();
        while ((line = reader.readLine()) != null) {
          //  output.append(line).append("\n");
            updateLog(line,output, logTextArea);
        }

        StringBuilder errorOutput = new StringBuilder();
        while ((line = errorReader.readLine()) != null) {
          //  errorOutput.append(line).append("\n");
            updateLog(line,errorOutput, logTextArea);
        }

        reader.close();
        errorReader.close();
        channelExec.disconnect();

        logger.info("Command Output: \n" + output.toString());
        if (errorOutput.length() > 0) {
            logger.error("Error Output: \n" + errorOutput.toString());

//            throw new Exception("Error Output: \n" + errorOutput.toString());
        }

        int exitStatus = channelExec.getExitStatus();
        if (exitStatus != 0) {
            logger.error("Command failed with exit status: " + exitStatus);
            throw new Exception("Error: Command failed with exit status: " + exitStatus );
        }
    }

    public String executeShellCommand(String command) throws Exception {
        if (session == null || !session.isConnected()) {
            throw new IllegalStateException("Session is not connected");
        }

        logger.info("Executing command: " + command);

        ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
        channelExec.setCommand(command);

        try (InputStream inputStream = channelExec.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             InputStream errorStream = channelExec.getErrStream();
             BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream))) {

            channelExec.connect();

            // Collect output
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // Collect error output
            StringBuilder errorOutput = new StringBuilder();
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }

            int exitStatus = channelExec.getExitStatus();
            logger.info("Value of exitStatus: " + exitStatus);

            //    logger.info("Command executed successfully with output:\n" + output);
            // Log and return based on command execution result
            if (exitStatus == 0) {
                logger.info("Command executed successfully with output:\n" + output);
                return output.length() > 0 ? output.toString().trim() : "No output from command.";
            } else {
                logger.error("Command failed with exit status: " + exitStatus);
                if (errorOutput.length() > 0) {
                    logger.error("Error Output:\n" + errorOutput);
                }
                //   throw new Exception("Command failed with exit status: " + exitStatus + "\nError Output:\n" + errorOutput);
            }

        } catch (IOException e) {
            logger.error("I/O error during command execution", e);
            return null;
            //   throw new Exception("I/O error occurred while executing command: " + e.getMessage(), e);
        } finally {
            close();
            if (channelExec != null && channelExec.isConnected()) {
                channelExec.disconnect();
            }
        }
        return  null;
    }

    public String executeBackgroundShellCommand(String command) throws Exception {
        if (session == null || !session.isConnected()) {
            throw new IllegalStateException("Session is not connected");
        }

        logger.info("Executing command: " + command);

        ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
        channelExec.setCommand(command);

        try (InputStream inputStream = channelExec.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
             InputStream errorStream = channelExec.getErrStream();
             BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorStream))) {

            channelExec.connect();

            // Collect output
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // Collect error output
            StringBuilder errorOutput = new StringBuilder();
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }

            int exitStatus = channelExec.getExitStatus();
        //    logger.info("Command executed successfully with output:\n" + output);
            // Log and return based on command execution result
            if (exitStatus == 0) {
                logger.info("Command executed successfully with output:\n" + output);
                return output.length() > 0 ? output.toString().trim() : "No output from command.";
            } else {
                logger.error("Command failed with exit status: " + exitStatus);
                if (errorOutput.length() > 0) {
                    logger.error("Error Output:\n" + errorOutput);
                }
             //   throw new Exception("Command failed with exit status: " + exitStatus + "\nError Output:\n" + errorOutput);
            }

        } catch (IOException e) {
            logger.error("I/O error during command execution", e);
            return null;
         //   throw new Exception("I/O error occurred while executing command: " + e.getMessage(), e);
        } finally {
            close();
            if (channelExec != null && channelExec.isConnected()) {
                channelExec.disconnect();
            }
        }
        return  null;
    }

}
