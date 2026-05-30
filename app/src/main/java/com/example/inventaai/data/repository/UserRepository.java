package com.example.inventaai.data.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.inventaai.data.db.DatabaseContract.UserEntry;
import com.example.inventaai.data.db.DatabaseHelper;
import com.example.inventaai.data.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class UserRepository {

    private static final String TAG = "InventaAi.UserRepo";
    private final DatabaseHelper dbHelper;

    public UserRepository(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    // =========================================================================
    // CRIAR USUÁRIO
    // =========================================================================

    /**
     * Cria um novo usuário com UUID gerado automaticamente.
     *
     * @param nome  Nome de exibição do usuário.
     * @param senha Senha em texto puro — será convertida em hash SHA-256.
     * @return User criado (com id e createdAt preenchidos), ou null em caso de erro.
     */
    public User createUser(String nome, String senha) {
        if (nomeJaExiste(nome)) {
            Log.w(TAG, "createUser: nome '" + nome + "' já em uso.");
            return null;
        }

        String id        = UUID.randomUUID().toString();
        String senhaHash = hashSHA256(senha);
        String hoje      = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        User user = new User(id, nome, senhaHash, hoje);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            ContentValues cv = toContentValues(user);
            long resultado   = db.insert(UserEntry.TABLE_NAME, null, cv);
            if (resultado == -1) {
                Log.e(TAG, "createUser: falha ao inserir usuário.");
                return null;
            }
            Log.d(TAG, "createUser: usuário '" + nome + "' criado com id=" + id);
            return user;
        } catch (Exception e) {
            Log.e(TAG, "createUser: erro", e);
            return null;
        } finally {
            db.close();
        }
    }

    // =========================================================================
    // LOGIN
    // =========================================================================

    /**
     * Valida credenciais comparando o hash da senha informada com o armazenado.
     *
     * @param nome  Nome do usuário.
     * @param senha Senha em texto puro.
     * @return User autenticado, ou null se as credenciais forem inválidas.
     */
    public User login(String nome, String senha) {
        String senhaHash = hashSHA256(senha);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    UserEntry.TABLE_NAME,
                    null,
                    UserEntry.COLUMN_NOME + " = ? AND " + UserEntry.COLUMN_SENHA_HASH + " = ?",
                    new String[]{ nome, senhaHash },
                    null, null, null, "1"
            );
            if (cursor.moveToFirst()) {
                User user = fromCursor(cursor);
                Log.d(TAG, "login: sucesso para nome='" + nome + "'.");
                return user;
            } else {
                Log.w(TAG, "login: credenciais inválidas para nome='" + nome + "'.");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "login: erro", e);
            return null;
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
    }

    // =========================================================================
    // BUSCAR POR ID
    // =========================================================================

    /**
     * Recupera um usuário pelo seu UUID.
     *
     * @param id UUID do usuário.
     * @return User encontrado, ou null.
     */
    public User getUserById(String id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    UserEntry.TABLE_NAME,
                    null,
                    UserEntry._ID + " = ?",
                    new String[]{ id },
                    null, null, null, "1"
            );
            if (cursor.moveToFirst()) {
                return fromCursor(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "getUserById: erro para id=" + id, e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return null;
    }

    // =========================================================================
    // ATUALIZAR NOME
    // =========================================================================

    /**
     * Atualiza o nome de exibição do usuário.
     *
     * @return true se a atualização foi bem-sucedida.
     */
    public boolean updateNome(String userId, String novoNome) {
        // Verifica se o novo nome já está em uso por outro usuário
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(UserEntry.TABLE_NAME,
                    new String[]{ UserEntry._ID },
                    UserEntry.COLUMN_NOME + " = ? AND " + UserEntry._ID + " != ?",
                    new String[]{ novoNome, userId },
                    null, null, null, "1");
            if (cursor.moveToFirst()) {
                Log.w(TAG, "updateNome: nome '" + novoNome + "' já em uso por outro usuário.");
                return false;
            }
        } finally {
            if (cursor != null) cursor.close();
        }

        try {
            ContentValues cv = new ContentValues();
            cv.put(UserEntry.COLUMN_NOME, novoNome);
            int linhas = db.update(UserEntry.TABLE_NAME, cv,
                    UserEntry._ID + " = ?", new String[]{ userId });
            Log.d(TAG, "updateNome: " + linhas + " linha(s) atualizada(s).");
            return linhas > 0;
        } catch (Exception e) {
            Log.e(TAG, "updateNome: erro", e);
            return false;
        } finally {
            db.close();
        }
    }

    // =========================================================================
    // ATUALIZAR SENHA
    // =========================================================================

    /**
     * Troca a senha do usuário após validar a senha atual.
     *
     * @param userId      UUID do usuário.
     * @param senhaAtual  Senha atual em texto puro.
     * @param novaSenha   Nova senha em texto puro.
     * @return true se a senha foi trocada com sucesso.
     */
    public boolean updateSenha(String userId, String senhaAtual, String novaSenha) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = null;
        try {
            // Valida senha atual
            String hashAtual = hashSHA256(senhaAtual);
            cursor = db.query(UserEntry.TABLE_NAME,
                    new String[]{ UserEntry._ID },
                    UserEntry._ID + " = ? AND " + UserEntry.COLUMN_SENHA_HASH + " = ?",
                    new String[]{ userId, hashAtual },
                    null, null, null, "1");
            if (!cursor.moveToFirst()) {
                Log.w(TAG, "updateSenha: senha atual incorreta para userId=" + userId);
                return false;
            }
        } finally {
            if (cursor != null) cursor.close();
        }

        try {
            ContentValues cv = new ContentValues();
            cv.put(UserEntry.COLUMN_SENHA_HASH, hashSHA256(novaSenha));
            int linhas = db.update(UserEntry.TABLE_NAME, cv,
                    UserEntry._ID + " = ?", new String[]{ userId });
            Log.d(TAG, "updateSenha: senha atualizada para userId=" + userId);
            return linhas > 0;
        } catch (Exception e) {
            Log.e(TAG, "updateSenha: erro", e);
            return false;
        } finally {
            db.close();
        }
    }

    // =========================================================================
    // ATUALIZAR AVATAR
    // =========================================================================

    /**
     * Atualiza o caminho da foto de perfil no armazenamento interno.
     *
     * @param userId UUID do usuário.
     * @param path   Caminho absoluto do arquivo de imagem.
     * @return true se atualizado com sucesso.
     */
    public boolean updateAvatar(String userId, String path) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put(UserEntry.COLUMN_AVATAR, path);
            int linhas = db.update(UserEntry.TABLE_NAME, cv,
                    UserEntry._ID + " = ?", new String[]{ userId });
            Log.d(TAG, "updateAvatar: caminho '" + path + "' salvo para userId=" + userId);
            return linhas > 0;
        } catch (Exception e) {
            Log.e(TAG, "updateAvatar: erro", e);
            return false;
        } finally {
            db.close();
        }
    }

    // =========================================================================
    // HELPERS PRIVADOS
    // =========================================================================

    /** Verifica se já existe um usuário com o nome informado. */
    private boolean nomeJaExiste(String nome) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(UserEntry.TABLE_NAME,
                    new String[]{ UserEntry._ID },
                    UserEntry.COLUMN_NOME + " = ?",
                    new String[]{ nome },
                    null, null, null, "1");
            return cursor.moveToFirst();
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
    }

    private ContentValues toContentValues(User user) {
        ContentValues cv = new ContentValues();
        cv.put(UserEntry._ID,               user.getId());
        cv.put(UserEntry.COLUMN_NOME,       user.getNome());
        cv.put(UserEntry.COLUMN_SENHA_HASH, user.getSenhaHash());
        cv.put(UserEntry.COLUMN_AVATAR,     user.getAvatarPath());
        cv.put(UserEntry.COLUMN_CREATED_AT, user.getCreatedAt());
        return cv;
    }

    private User fromCursor(Cursor cursor) {
        return new User(
                cursor.getString(cursor.getColumnIndexOrThrow(UserEntry._ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(UserEntry.COLUMN_NOME)),
                cursor.getString(cursor.getColumnIndexOrThrow(UserEntry.COLUMN_SENHA_HASH)),
                cursor.getString(cursor.getColumnIndexOrThrow(UserEntry.COLUMN_AVATAR)),
                cursor.getString(cursor.getColumnIndexOrThrow(UserEntry.COLUMN_CREATED_AT))
        );
    }

    /**
     * Gera o hash SHA-256 da string informada.
     * Retorna uma string hexadecimal de 64 caracteres.
     */
    public static String hashSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 está disponível em todas as versões do Android — não ocorrerá na prática
            throw new RuntimeException("SHA-256 não disponível", e);
        }
    }
}