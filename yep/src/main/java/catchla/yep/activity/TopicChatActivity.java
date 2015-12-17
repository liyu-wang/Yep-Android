package catchla.yep.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;

import com.desmond.asyncmanager.AsyncManager;
import com.desmond.asyncmanager.TaskRunnable;

import org.apache.commons.lang3.StringUtils;

import catchla.yep.Constants;
import catchla.yep.R;
import catchla.yep.fragment.TopicChatListFragment;
import catchla.yep.model.TaskResponse;
import catchla.yep.model.Topic;
import catchla.yep.model.UrlResponse;
import catchla.yep.model.YepException;
import catchla.yep.util.MenuUtils;
import catchla.yep.util.Utils;
import catchla.yep.util.YepAPI;
import catchla.yep.util.YepAPIFactory;

public class TopicChatActivity extends SwipeBackContentActivity implements Constants {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_chat);

        final Topic topic = getTopic();
        displayTopic(topic);

        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        final Bundle args = new Bundle();
        args.putParcelable(EXTRA_ACCOUNT, getAccount());
        args.putParcelable(EXTRA_TOPIC, topic);
        ft.replace(R.id.chat_list, Fragment.instantiate(this, TopicChatListFragment.class.getName(), args));
        ft.commit();
    }

    private Topic getTopic() {
        return getIntent().getParcelableExtra(EXTRA_TOPIC);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_topic_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share: {
                AsyncManager.runBackgroundTask(new TaskRunnable<Object, TaskResponse<UrlResponse>,
                        TopicChatActivity>() {
                    @Override
                    public TaskResponse<UrlResponse> doLongOperation(final Object o) throws InterruptedException {
                        YepAPI yep = YepAPIFactory.getInstance(TopicChatActivity.this, getAccount());
                        try {
                            return TaskResponse.getInstance(yep.getCircleShareUrl(getTopic().getCircle().getId()));
                        } catch (YepException e) {
                            return TaskResponse.getInstance(e);
                        }
                    }


                });
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        final boolean isMyTopic = StringUtils.equals(getTopic().getUser().getId(), Utils.getAccountId(this, getAccount()));
        MenuUtils.setMenuGroupAvailability(menu, R.id.group_menu_my_topic, isMyTopic);
        return super.onPrepareOptionsMenu(menu);
    }

    private void displayTopic(final Topic topic) {
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
    }
}
