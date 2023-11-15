package org.profinef.service;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Класс предназначен для обработки данных в базе и работы с бекапом
 */
@Service
public final class DatabaseUtil {

    private static final Logger logger =  LoggerFactory.getLogger(DatabaseUtil.class);

    /**
     * Метод создает локальную копию базы данных с заданными параметрами для дальнейшего возможного отката изменений
     * базы данных
     * @param dbUsername имя пользователя БД
     * @param dbPassword пароль БД
     * @param dbName название БД
     * @param outputFile название и путь к файлу, в который необходимо записать бекап
     * @return статус выполнения по завершению (true/false)
     */
    public static boolean backupLocal(String dbUsername, String dbPassword, String dbName, String outputFile) {
        try {
            String command = String.format("C:\\Program Files\\MySQL\\MySQL Workbench 8.0\\mysqldump -u%s -p%s --add-drop-table --databases %s -r %s",
                    dbUsername, dbPassword, dbName, outputFile);
            Process process = Runtime.getRuntime().exec(command);
            int processComplete = process.waitFor();
            return processComplete == 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Метод создает копию файла бекапа (или любого другого по необходимости) на сетевом удаленном хранилище по указанным
     * параметрам
     * @param hostname имя хоста сети
     * @param username имя пользователя в сети
     * @param password пароль в сети
     * @param domain сетевой домен
     * @param shareName имя удаленного хранилища
     * @param inputFile файл, который необходимо передать
     */
    public static void copyBackup(String hostname, String username, String password, String domain, String shareName, String inputFile) {
        SMBClient client = new SMBClient();
        // создаем соединение с хостом
        try (Connection connection = client.connect(hostname)) {
            // проходим аутентификацию
            AuthenticationContext ac = new AuthenticationContext(username, password.toCharArray(), domain);
            Session session = connection.authenticate(ac);
            FileInputStream input = new FileInputStream(inputFile);
            // коннектимся к сетевому хранилищу
            try (DiskShare share = (DiskShare) session.connectShare(shareName)) {
                Set<FileAttributes> fileAttributes = new HashSet<>();
                fileAttributes.add(FileAttributes.FILE_ATTRIBUTE_NORMAL);
                Set<SMB2CreateOptions> createOptions = new HashSet<>();
                createOptions.add(SMB2CreateOptions.FILE_RANDOM_ACCESS);
                // отправляем файл
                File f = share.openFile(inputFile, new HashSet<>(List.of(AccessMask.GENERIC_ALL)), fileAttributes, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OVERWRITE_IF, createOptions);

                OutputStream oStream = f.getOutputStream();
                oStream.write(input.readAllBytes());
                oStream.flush();
                oStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод восстанавливает базу данных на основании ранее созданного файла бекапа
     * @param dbUsername имя пользователя БД
     * @param dbPassword пароль БД
     * @param dbName название БД
     * @param inputFile файл бекапа для восстановления
     * @return статус выполнения по завершению (true/false)
     */
    public static boolean restore(String dbUsername, String dbPassword, String dbName, String inputFile) {
        try {
            String[] command= new String[]{"C:\\Program Files\\MySQL\\MySQL Workbench 8.0\\mysql", "--user=" + dbUsername, "--password=" + dbPassword, dbName,"-e", " source "+ inputFile};
            Process process = Runtime.getRuntime().exec(command);
            int processComplete = process.waitFor();
            return processComplete == 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
