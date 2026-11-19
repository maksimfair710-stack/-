/*
 * LIQRA compatibility implementation for the open AyuGram4A source tree.
 *
 * This file replaces the inaccessible proprietary AyuMessageUtils dependency.
 * It stores Telegram TL objects using their native TL serialization format.
 * Copyright (C) 2026 LIQRA contributors.
 * Licensed under the same GPL-2.0-or-later terms as the AyuGram4A project.
 */
package com.radolyn.ayugram.proprietary;

import com.radolyn.ayugram.AyuConstants;
import com.radolyn.ayugram.database.entities.AyuMessageBase;
import com.radolyn.ayugram.database.entities.EditedMessage;
import com.radolyn.ayugram.messages.AyuMessagesController;
import com.radolyn.ayugram.messages.AyuSavePreferences;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.AbstractSerializedData;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class AyuMessageUtils {
    private static final int VECTOR_MAGIC = 0x1cb5c415;

    private AyuMessageUtils() { }

    public static void map(AyuSavePreferences preferences, AyuMessageBase target) {
        if (preferences == null || target == null || preferences.getMessage() == null) return;
        TLRPC.Message message = preferences.getMessage();

        target.userId = preferences.getUserId();
        target.dialogId = preferences.getDialogId();
        target.topicId = preferences.getTopicId();
        target.messageId = preferences.getMessageId();
        target.entityCreateDate = preferences.getRequestCatchTime();

        target.date = intValue(message, "date");
        target.flags = intValue(message, "flags");
        target.editDate = intValue(message, "edit_date");
        target.views = intValue(message, "views");
        target.groupedId = longValue(message, "grouped_id");
        target.text = stringValue(message, "message");
        target.textEntities = serializeVector(get(message, "entities"));

        target.peerId = peerId(get(message, "peer_id"));
        target.fromId = peerId(get(message, "from_id"));

        Object forward = get(message, "fwd_from");
        if (forward != null) {
            target.fwdFlags = intValue(forward, "flags");
            target.fwdFromId = peerId(get(forward, "from_id"));
            target.fwdName = stringValue(forward, "from_name");
            target.fwdDate = intValue(forward, "date");
            target.fwdPostAuthor = stringValue(forward, "post_author");
        }

        Object reply = get(message, "reply_to");
        if (reply != null) {
            target.replyFlags = intValue(reply, "flags");
            target.replyMessageId = intValue(reply, "reply_to_msg_id");
            target.replyPeerId = peerId(get(reply, "reply_to_peer_id"));
            target.replyTopId = intValue(reply, "reply_to_top_id");
            target.replyForumTopic = booleanValue(reply, "forum_topic");
        }
    }

    public static void mapMedia(AyuSavePreferences preferences, AyuMessageBase target, boolean copyMedia) {
        if (preferences == null || target == null || preferences.getMessage() == null) return;
        TLRPC.Message message = preferences.getMessage();
        Object media = get(message, "media");
        target.documentSerialized = serializeObject(media);
        target.documentType = documentType(message, media);
        Object document = get(media, "document");
        target.mimeType = stringValue(document, "mime_type");

        File source = null;
        try {
            source = FileLoader.getInstance(preferences.getAccountId()).getPathToMessage(message);
        } catch (Throwable ignored) { }

        if (!copyMedia) {
            try {
                List<EditedMessage> revisions = AyuMessagesController.getInstance().getRevisions(
                        preferences.getUserId(), preferences.getDialogId(), preferences.getMessageId());
                if (revisions != null && !revisions.isEmpty()) {
                    String previousPath = revisions.get(revisions.size() - 1).mediaPath;
                    if (previousPath != null && !previousPath.isEmpty()) {
                        target.mediaPath = previousPath;
                        return;
                    }
                }
            } catch (Throwable ignored) { }
        }

        if (source == null) return;
        target.mediaPath = source.getAbsolutePath();
        if (copyMedia && source.isFile()) {
            try {
                File folder = AyuMessagesController.attachmentsPath;
                if (!folder.exists() && !folder.mkdirs()) return;
                File destination = new File(folder,
                        preferences.getDialogId() + "_" + preferences.getMessageId() + "_" + source.getName());
                copyFile(source, destination);
                target.mediaPath = destination.getAbsolutePath();
            } catch (Throwable ignored) { }
        }
    }

    public static void map(EditedMessage source, TLRPC.Message target, int currentAccount) {
        if (source == null || target == null) return;
        set(target, "id", source.messageId);
        set(target, "dialog_id", source.dialogId);
        set(target, "date", source.date);
        set(target, "flags", source.flags);
        set(target, "edit_date", source.editDate);
        set(target, "views", source.views);
        set(target, "grouped_id", source.groupedId);
        set(target, "message", source.text == null ? "" : source.text);
        set(target, "entities", deserializeVector(source.textEntities));
        set(target, "peer_id", makePeer(source.peerId, currentAccount));
        set(target, "from_id", makePeer(source.fromId, currentAccount));
        setForwardHeader(source, target, currentAccount);
        setReplyHeader(source, target, currentAccount);
    }

    public static void mapMedia(EditedMessage source, TLRPC.Message target) {
        if (source == null || target == null || source.documentSerialized == null) return;
        Object media = deserializeObject(TLRPC.MessageMedia.class, source.documentSerialized);
        if (media != null) {
            set(target, "media", media);
        }
        if (source.mediaPath != null) {
            // Some Telegram message variants expose attachPath; variants that do not are ignored.
            set(target, "attachPath", source.mediaPath);
        }
    }

    private static int documentType(TLRPC.Message message, Object media) {
        if (media == null) return AyuConstants.DOCUMENT_TYPE_NONE;
        if (get(media, "photo") != null) return AyuConstants.DOCUMENT_TYPE_PHOTO;
        if (get(media, "document") != null) {
            try {
                if (org.telegram.messenger.MessageObject.isStickerMessage(message)) {
                    return AyuConstants.DOCUMENT_TYPE_STICKER;
                }
            } catch (Throwable ignored) { }
            return AyuConstants.DOCUMENT_TYPE_FILE;
        }
        return AyuConstants.DOCUMENT_TYPE_NONE;
    }

    private static void setForwardHeader(EditedMessage source, TLRPC.Message target, int account) {
        if (source.fwdDate == 0 && source.fwdFromId == 0 &&
                (source.fwdName == null || source.fwdName.isEmpty())) return;
        Object header = create("org.telegram.tgnet.TLRPC$TL_messageFwdHeader");
        if (header == null) return;
        set(header, "flags", source.fwdFlags);
        set(header, "from_id", makePeer(source.fwdFromId, account));
        set(header, "from_name", source.fwdName);
        set(header, "date", source.fwdDate);
        set(header, "post_author", source.fwdPostAuthor);
        set(target, "fwd_from", header);
    }

    private static void setReplyHeader(EditedMessage source, TLRPC.Message target, int account) {
        if (source.replyMessageId == 0 && source.replyPeerId == 0 && source.replyTopId == 0) return;
        Object header = create("org.telegram.tgnet.TLRPC$TL_messageReplyHeader");
        if (header == null) return;
        set(header, "flags", source.replyFlags);
        set(header, "reply_to_msg_id", source.replyMessageId);
        set(header, "reply_to_peer_id", makePeer(source.replyPeerId, account));
        set(header, "reply_to_top_id", source.replyTopId);
        set(header, "forum_topic", source.replyForumTopic);
        set(target, "reply_to", header);
    }

    private static Object makePeer(long id, int account) {
        if (id == 0) return null;
        String className;
        String fieldName;
        if (id > 0) {
            className = "org.telegram.tgnet.TLRPC$TL_peerUser";
            fieldName = "user_id";
        } else {
            boolean channel = false;
            try {
                TLRPC.Chat chat = MessagesController.getInstance(account).getChat((int) -id);
                channel = chat != null && chat.channel;
            } catch (Throwable ignored) { }
            className = channel ? "org.telegram.tgnet.TLRPC$TL_peerChannel" : "org.telegram.tgnet.TLRPC$TL_peerChat";
            fieldName = channel ? "channel_id" : "chat_id";
        }
        Object peer = create(className);
        if (peer != null) set(peer, fieldName, Math.abs(id));
        return peer;
    }

    private static long peerId(Object peer) {
        if (peer == null) return 0;
        long user = longValue(peer, "user_id");
        if (user != 0) return user;
        long channel = longValue(peer, "channel_id");
        if (channel != 0) return -channel;
        long chat = longValue(peer, "chat_id");
        return chat == 0 ? 0 : -chat;
    }

    private static byte[] serializeVector(Object value) {
        if (!(value instanceof Iterable<?>)) return null;
        ArrayList<?> items = new ArrayList<>();
        for (Object item : (Iterable<?>) value) items.add(item);
        SerializedData data = new SerializedData();
        data.writeInt32(VECTOR_MAGIC);
        data.writeInt32(items.size());
        for (Object item : items) {
            if (item instanceof TLObject) ((TLObject) item).serializeToStream(data);
        }
        return data.toByteArray();
    }

    private static ArrayList<TLRPC.MessageEntity> deserializeVector(byte[] bytes) {
        ArrayList<TLRPC.MessageEntity> result = new ArrayList<>();
        if (bytes == null || bytes.length == 0) return result;
        SerializedData data = new SerializedData(bytes);
        try {
            if (data.readInt32(true) != VECTOR_MAGIC) return result;
            int count = data.readInt32(true);
            if (count < 0 || count > 10000) return result;
            for (int i = 0; i < count; i++) {
                int constructor = data.readInt32(true);
                TLRPC.MessageEntity entity = TLRPC.MessageEntity.TLdeserialize(data, constructor, true);
                if (entity != null) result.add(entity);
            }
        } catch (Throwable ignored) {
            result.clear();
        } finally {
            data.cleanup();
        }
        return result;
    }

    private static byte[] serializeObject(Object value) {
        if (!(value instanceof TLObject)) return null;
        SerializedData data = new SerializedData();
        ((TLObject) value).serializeToStream(data);
        return data.toByteArray();
    }

    private static Object deserializeObject(Class<?> type, byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        SerializedData data = new SerializedData(bytes);
        try {
            int constructor = data.readInt32(true);
            Method method = type.getMethod("TLdeserialize", AbstractSerializedData.class, int.class, boolean.class);
            return method.invoke(null, data, constructor, true);
        } catch (Throwable ignored) {
            return null;
        } finally {
            data.cleanup();
        }
    }

    private static void copyFile(File source, File destination) throws Exception {
        if (source.getCanonicalFile().equals(destination.getCanonicalFile())) return;
        File parent = destination.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (FileInputStream input = new FileInputStream(source);
             FileOutputStream output = new FileOutputStream(destination)) {
            byte[] buffer = new byte[32 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            output.flush();
        }
    }

    private static Object create(String className) {
        try {
            return Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object get(Object instance, String name) {
        if (instance == null) return null;
        Field field = findField(instance.getClass(), name);
        if (field == null) return null;
        try {
            field.setAccessible(true);
            return field.get(instance);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean set(Object instance, String name, Object value) {
        if (instance == null) return false;
        Field field = findField(instance.getClass(), name);
        if (field == null) return false;
        try {
            field.setAccessible(true);
            if (value == null) {
                if (!field.getType().isPrimitive()) field.set(instance, null);
                return true;
            }
            Class<?> type = field.getType();
            if (type == int.class || type == Integer.class) field.set(instance, ((Number) value).intValue());
            else if (type == long.class || type == Long.class) field.set(instance, ((Number) value).longValue());
            else if (type == boolean.class || type == Boolean.class) field.set(instance, value instanceof Boolean ? value : Boolean.parseBoolean(String.valueOf(value)));
            else if (type == String.class) field.set(instance, String.valueOf(value));
            else field.set(instance, value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Field findField(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) { }
        }
        return null;
    }

    private static int intValue(Object object, String field) {
        Object value = get(object, field);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private static long longValue(Object object, String field) {
        Object value = get(object, field);
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private static String stringValue(Object object, String field) {
        Object value = get(object, field);
        return value == null ? null : String.valueOf(value);
    }

    private static boolean booleanValue(Object object, String field) {
        Object value = get(object, field);
        return value instanceof Boolean && (Boolean) value;
    }
}
