package top.xihale.clouddoc.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * 密码加密与校验工具类
 */
public class PasswordUtils {

    private PasswordUtils() {
    }

    /**
     * 对密码进行 BCrypt 加密
     */
    public static String hash(String password) {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray());
    }

    /**
     * 校验密码是否匹配
     */
    public static boolean verify(String password, String hash) {
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified;
    }
}
