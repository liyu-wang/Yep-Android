package catchla.yep.service;

import android.accounts.Account;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.squareup.otto.Bus;

import org.mariotaku.abstask.library.AbstractTask;
import org.mariotaku.abstask.library.TaskStarter;
import org.mariotaku.sqliteqb.library.Expression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import catchla.yep.BuildConfig;
import catchla.yep.Constants;
import catchla.yep.message.FriendshipsRefreshedEvent;
import catchla.yep.message.MessageRefreshedEvent;
import catchla.yep.model.Circle;
import catchla.yep.model.CircleCursorIndices;
import catchla.yep.model.CircleTableInfo;
import catchla.yep.model.Conversation;
import catchla.yep.model.ConversationValuesCreator;
import catchla.yep.model.ConversationsResponse;
import catchla.yep.model.Friendship;
import catchla.yep.model.Message;
import catchla.yep.model.MessageValuesCreator;
import catchla.yep.model.Paging;
import catchla.yep.model.ResponseList;
import catchla.yep.model.TaskResponse;
import catchla.yep.model.User;
import catchla.yep.model.YepException;
import catchla.yep.provider.YepDataStore.Circles;
import catchla.yep.provider.YepDataStore.Conversations;
import catchla.yep.provider.YepDataStore.Friendships;
import catchla.yep.provider.YepDataStore.Messages;
import catchla.yep.util.ContentResolverUtils;
import catchla.yep.util.ContentValuesCreator;
import catchla.yep.util.Utils;
import catchla.yep.util.YepAPI;
import catchla.yep.util.YepAPIFactory;
import catchla.yep.util.dagger.GeneralComponentHelper;

/**
 * Created by mariotaku on 15/5/29.
 */
public class MessageService extends Service implements Constants {

    public static final String ACTION_PREFIX = BuildConfig.APPLICATION_ID + ".";
    public static final String ACTION_REFRESH_MESSAGES = ACTION_PREFIX + "REFRESH_MESSAGES";
    public static final String ACTION_REFRESH_USER_INFO = ACTION_PREFIX + "REFRESH_USER_INFO";
    public static final String ACTION_REFRESH_FRIENDSHIPS = ACTION_PREFIX + "REFRESH_FRIENDSHIPS";

    @Inject
    Bus mBus;

    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        GeneralComponentHelper.build(this).inject(this);
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (intent == null) return START_NOT_STICKY;
        final String action = intent.getAction();
        if (action == null) return START_NOT_STICKY;
        switch (action) {
            case ACTION_REFRESH_FRIENDSHIPS: {
                final Account account = intent.getParcelableExtra(EXTRA_ACCOUNT);
                refreshFriendships(account);
                break;
            }
            case ACTION_REFRESH_MESSAGES: {
                refreshCircles();
                refreshMessages();
                break;
            }
            case ACTION_REFRESH_USER_INFO: {
                final Account account = intent.getParcelableExtra(EXTRA_ACCOUNT);
                refreshUserInfo(account);
                break;
            }
        }
        return START_STICKY;
    }

    private void refreshUserInfo(final Account account) {
        if (account == null) return;

        final AbstractTask<Account, TaskResponse<User>, MessageService>
                task = new AbstractTask<Account, TaskResponse<User>, MessageService>() {

            @Override
            public TaskResponse<User> doLongOperation(final Account account) {
                YepAPI yep = YepAPIFactory.getInstance(getApplication(), account);
                try {
                    return TaskResponse.getInstance(yep.getUser());
                } catch (YepException e) {
                    return TaskResponse.getInstance(e);
                }
            }

            @Override
            public void afterExecute(final MessageService handler, final TaskResponse<User> result) {
                if (result.hasData()) {
                    Utils.saveUserInfo(handler, account, result.getData());
                }
            }
        };
        TaskStarter.execute(task);
    }

    private void refreshFriendships(final Account account) {
        if (account == null) return;
        final AbstractTask<Account, TaskResponse<Boolean>, MessageService>
                task = new AbstractTask<Account, TaskResponse<Boolean>, MessageService>() {
            @Override
            public TaskResponse<Boolean> doLongOperation(final Account account) {
                final YepAPI yep = YepAPIFactory.getInstance(getApplication(), account);
                try {
                    ResponseList<Friendship> friendships;
                    int page = 1;
                    final Paging paging = new Paging();
                    final ArrayList<ContentValues> values = new ArrayList<>();
                    final String accountId = Utils.getAccountId(getApplication(), account);
                    while ((friendships = yep.getFriendships(paging)).size() > 0) {
                        for (Friendship friendship : friendships) {
                            values.add(ContentValuesCreator.fromFriendship(friendship, accountId));
                        }
                        paging.page(++page);
                        if (friendships.getCount() < friendships.getPerPage()) break;
                    }

                    final ContentResolver cr = getContentResolver();
                    cr.delete(Friendships.CONTENT_URI, null, null);
                    ContentResolverUtils.bulkInsert(cr, Friendships.CONTENT_URI, values);
                    return TaskResponse.getInstance(true);
                } catch (YepException e) {
                    Log.w(LOGTAG, e);
                    return TaskResponse.getInstance(e);
                } finally {
                }
            }

            @Override
            public void afterExecute(final TaskResponse<Boolean> response) {
                mBus.post(new FriendshipsRefreshedEvent());
            }

            @Override
            public void afterExecute(final MessageService messageService, final TaskResponse<Boolean> response) {
                afterExecute(response);
            }
        };
        task.setParams(account);
        task.setResultHandler(this);
        TaskStarter.execute(task);
    }

    private void refreshMessages() {
        final Account account = Utils.getCurrentAccount(this);
        if (account == null) return;
        final User accountUser = Utils.getAccountUser(this, account);
        if (accountUser == null) return;
        final AbstractTask<Account, TaskResponse<Boolean>, MessageService>
                task = new AbstractTask<Account, TaskResponse<Boolean>, MessageService>() {
            @Override
            public TaskResponse<Boolean> doLongOperation(final Account account) {
                final YepAPI yep = YepAPIFactory.getInstance(getApplication(), account);
                try {
                    Paging paging = new Paging();
                    paging.perPage(30);
                    ConversationsResponse conversations = yep.getConversations(paging);
                    insertConversations(MessageService.this, conversations, accountUser.getId());
                    System.identityHashCode(conversations);
                    return TaskResponse.getInstance(true);
                } catch (YepException e) {
                    Log.w(LOGTAG, e);
                    return TaskResponse.getInstance(e);
                } catch (Throwable e) {
                    Log.wtf(LOGTAG, e);
                    return TaskResponse.getInstance(e);
                } finally {
                }
            }

            @Override
            public void afterExecute(final TaskResponse<Boolean> response) {
                mBus.post(new MessageRefreshedEvent());
            }

            @Override
            public void afterExecute(final MessageService messageService, final TaskResponse<Boolean> response) {
                afterExecute(response);
            }
        };
        task.setParams(account);
        task.setResultHandler(this);
        TaskStarter.execute(task);
    }

    private void refreshCircles() {
        final Account account = Utils.getCurrentAccount(this);
        if (account == null) return;
        final User accountUser = Utils.getAccountUser(this, account);
        if (accountUser == null) return;
        final AbstractTask<Account, TaskResponse<Boolean>, MessageService>
                task = new AbstractTask<Account, TaskResponse<Boolean>, MessageService>() {
            @Override
            public TaskResponse<Boolean> doLongOperation(final Account account) {
                final YepAPI yep = YepAPIFactory.getInstance(getApplication(), account);
                try {
                    Paging paging = new Paging();
                    ResponseList<Circle> circles = yep.getCircles(paging);
                    final String accountId = accountUser.getId();
                    insertCircles(MessageService.this, circles, accountId);
                    return TaskResponse.getInstance(true);
                } catch (YepException e) {
                    Log.w(LOGTAG, e);
                    return TaskResponse.getInstance(e);
                } finally {
                }
            }

            @Override
            public void afterExecute(final TaskResponse<Boolean> response) {
                mBus.post(new MessageRefreshedEvent());
            }

            @Override
            public void afterExecute(final MessageService messageService, final TaskResponse<Boolean> response) {
                afterExecute(response);
            }
        };
        task.setParams(account);
        task.setResultHandler(this);
        TaskStarter.execute(task);
    }

    public static void insertCircles(final Context context, final Collection<Circle> circles, final String accountId) {
        final ContentResolver cr = context.getContentResolver();
        cr.delete(Circles.CONTENT_URI, null, null);
        final List<ContentValues> contentValues = new ArrayList<>();
        for (final Circle circle : circles) {
            contentValues.add(ContentValuesCreator.fromCircle(circle, accountId));
        }
        ContentResolverUtils.bulkInsert(cr, Circles.CONTENT_URI, contentValues);
    }

    public static void insertConversations(final Context context, final ConversationsResponse conversations,
                                           final String accountId) {
        final HashMap<String, Conversation> conversationsMap = new HashMap<>();
        final ContentResolver cr = context.getContentResolver();
        final Set<String> conversationIds = new HashSet<>();
        final List<Message> messages = conversations.getMessages();
        final Map<String, User> users = new HashMap<>();
        for (final User user : conversations.getUsers()) {
            users.put(user.getId(), user);
        }
        final ContentValues[] messageValues = new ContentValues[messages.size()];
        final String[] messageIds = new String[messages.size()];
        final List<String> randomIds = new ArrayList<>();
        for (int i = 0, j = messages.size(); i < j; i++) {
            final Message message = messages.get(i);
            final String recipientType = message.getRecipientType();
            final String conversationId = Conversation.generateId(message, accountId);
            message.setAccountId(accountId);
            message.setConversationId(conversationId);

            final ContentValues values = MessageValuesCreator.create(message);
            messageValues[i] = values;
            messageIds[i] = message.getId();
            final String randomId = message.getRandomId();
            if (randomId != null) {
                randomIds.add(randomId);
            }

            Conversation conversation = conversationsMap.get(conversationId);
            final boolean newConversation = conversation == null;
            if (conversation == null) {
                conversation = Conversation.query(cr, accountId, conversationId);
                if (conversation == null) {
                    conversation = new Conversation();
                    conversation.setAccountId(accountId);
                    conversation.setId(conversationId);
                }
            }
            final Date createdAt = message.getCreatedAt();
            if (newConversation || greaterThen(createdAt, conversation.getUpdatedAt())) {
                conversation.setTextContent(message.getTextContent());
                final User sender = message.getSender();
                if (Message.RecipientType.USER.equals(recipientType)) {
                    // Outgoing
                    if (!TextUtils.equals(accountId, sender.getId())) {
                        conversation.setUser(sender);
                    } else {
                        final User user = users.get(message.getRecipientId());
                        if (user != null) {
                            conversation.setUser(user);
                        }
                    }
                } else {
                    conversation.setUser(sender);
                }
                conversation.setSender(sender);
                conversation.setCircle(getMessageCircle(context, message, conversations, accountId));
                conversation.setUpdatedAt(createdAt);
                conversation.setRecipientType(recipientType);
                conversation.setMediaType(message.getMediaType());
            }
            if (newConversation && conversation.getUser() != null) {
                conversationIds.add(conversationId);
                conversationsMap.put(conversationId, conversation);
            }
        }

        ContentResolverUtils.bulkDelete(cr, Conversations.CONTENT_URI, Conversations.CONVERSATION_ID,
                conversationIds, Expression.equalsArgs(Conversations.ACCOUNT_ID).getSQL(), new String[]{accountId});
        List<ContentValues> conversationValues = new ArrayList<>();
        for (final Conversation conversation : conversationsMap.values()) {
            conversationValues.add(ConversationValuesCreator.create(conversation));
        }
        ContentResolverUtils.bulkInsert(cr, Conversations.CONTENT_URI, conversationValues);

        ContentResolverUtils.bulkDelete(cr, Messages.CONTENT_URI, Messages.RANDOM_ID,
                randomIds, Expression.equalsArgs(Messages.ACCOUNT_ID).getSQL(), new String[]{accountId});
        ContentResolverUtils.bulkDelete(cr, Messages.CONTENT_URI, Messages.MESSAGE_ID,
                messageIds, Expression.equalsArgs(Messages.ACCOUNT_ID).getSQL(), new String[]{accountId});
        ContentResolverUtils.bulkInsert(cr, Messages.CONTENT_URI, messageValues);
    }

    private static boolean greaterThen(final Date createdAt, final Date updatedAt) {
        if (updatedAt == null) return createdAt != null;
        return createdAt != null && createdAt.compareTo(updatedAt) > 0;
    }

    private static Circle getMessageCircle(final Context context, final Message message,
                                           final ConversationsResponse conversations,
                                           final String accountId) {
        if (!Message.RecipientType.CIRCLE.equals(message.getRecipientType())) return null;
        Circle circle = message.getCircle();
        // First try find in conversations
        if (conversations != null) {
            final List<Circle> circles = conversations.getCircles();
            if (circles != null) {
                for (final Circle item : circles) {
                    if (TextUtils.equals(item.getId(), message.getRecipientId())) {
                        return item;
                    }
                }
            }
        }
        // Then try to load from database
        final String circleId;
        if (circle == null) {
            circleId = message.getRecipientId();
        } else if (circle.getTopic() == null) {
            circleId = circle.getId();
        } else {
            return circle;
        }
        final String where = Expression.and(Expression.equalsArgs(Circles.ACCOUNT_ID),
                Expression.equalsArgs(Circles.CIRCLE_ID)).getSQL();
        String[] whereArgs = {accountId, circleId};
        final Cursor c = context.getContentResolver().query(Circles.CONTENT_URI,
                CircleTableInfo.COLUMNS, where, whereArgs, null);
        try {
            if (c != null && c.moveToFirst()) {
                final CircleCursorIndices ci = new CircleCursorIndices(c);
                circle = ci.newObject(c);
            }
        } finally {
            Utils.closeSilently(c);
        }
        return circle;
    }
}
