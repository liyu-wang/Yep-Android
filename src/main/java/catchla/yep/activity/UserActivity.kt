package catchla.yep.activity

import android.app.Dialog
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.DialogFragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.support.v7.app.AlertDialog
import android.support.v7.graphics.Palette
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import catchla.yep.Constants
import catchla.yep.R
import catchla.yep.loader.UserLoader
import catchla.yep.model.*
import catchla.yep.provider.YepDataStore.Friendships
import catchla.yep.util.*
import catchla.yep.util.task.UpdateProfileTask
import org.apache.commons.lang3.StringUtils
import org.apmem.tools.layouts.FlowLayout
import org.mariotaku.abstask.library.AbstractTask
import org.mariotaku.abstask.library.TaskStarter
import org.mariotaku.sqliteqb.library.Expression
import java.util.*

class UserActivity : SwipeBackContentActivity(), Constants, View.OnClickListener, LoaderManager.LoaderCallbacks<TaskResponse<User>>, UpdateProfileTask.Callback {

    private lateinit var actionButton: FloatingActionButton
    private lateinit var profileImageView: ImageView
    private lateinit var nameView: TextView
    private lateinit var usernameView: TextView
    private lateinit var introductionView: TextView
    private lateinit var masterSkills: FlowLayout
    private lateinit var learningSkills: FlowLayout
    private lateinit var masterLabel: View
    private lateinit var learningLabel: View
    private lateinit var providersContainer: LinearLayout
    private lateinit var providersDivider: View
    private lateinit var currentUser: User
    private lateinit var userScrollContent: View
    private lateinit var userScrollView: View
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var toolbar: Toolbar
    private lateinit var topicsItemView: View
    private lateinit var collapsingToolbar: View

    private var updateProfileTask: UpdateProfileTask? = null

    override fun onContentChanged() {
        super.onContentChanged()
        actionButton = findViewById(R.id.fab) as FloatingActionButton
        profileImageView = findViewById(R.id.profile_image) as ImageView
        toolbar = findViewById(R.id.toolbar) as Toolbar
        nameView = findViewById(R.id.name) as TextView
        usernameView = findViewById(R.id.username) as TextView
        introductionView = findViewById(R.id.introduction) as TextView
        masterSkills = findViewById(R.id.master_skills) as FlowLayout
        learningSkills = findViewById(R.id.learning_skills) as FlowLayout
        masterLabel = findViewById(R.id.master_label)!!
        learningLabel = findViewById(R.id.learning_label)!!
        providersContainer = findViewById(R.id.providers_container) as LinearLayout
        providersDivider = findViewById(R.id.providers_divider)!!
        userScrollContent = findViewById(R.id.user_scroll_content)!!
        userScrollView = findViewById(R.id.user_scroll_view)!!
        appBarLayout = findViewById(R.id.app_bar) as AppBarLayout
        topicsItemView = findViewById(R.id.user_topics)!!
        collapsingToolbar = findViewById(R.id.collapsing_toolbar)!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar!!
        actionBar.setDisplayHomeAsUpEnabled(true)


        val currentUser: User?
        val intent = intent
        val account = account

        if (intent.hasExtra(Constants.EXTRA_USER)) {
            currentUser = intent.getParcelableExtra<User>(Constants.EXTRA_USER)
        } else {
            currentUser = Utils.getAccountUser(this, account)
        }

        if (currentUser == null) {
            finish()
            return
        }
        actionButton.setOnClickListener(this)
        topicsItemView.setOnClickListener(this)

        title = Utils.getDisplayName(currentUser)
        displayUser(currentUser)

        supportLoaderManager.initLoader(0, null, this)
    }

    override fun onDestroy() {
        super.onDestroy()
    }


    private fun displayUser(user: User?) {
        if (user == null) return
        currentUser = user
        val avatarUrl = user.avatarUrl
        imageLoader.displayProfileImage(avatarUrl, profileImageView)
        val username = user.username
        val introduction = user.introduction
        nameView.text = user.nickname
        if (TextUtils.isEmpty(username)) {
            usernameView.setText(R.string.no_username)
        } else {
            usernameView.text = username
        }
        if (TextUtils.isEmpty(introduction)) {
            introductionView.setText(R.string.no_introduction_yet)
        } else {
            introductionView.text = introduction
        }

        val skillOnClickListener = View.OnClickListener { v ->
            val skill = v.tag as Skill
            val intent = Intent(this@UserActivity, SkillUsersActivity::class.java)
            intent.putExtra(Constants.EXTRA_ACCOUNT, account)
            intent.putExtra(Constants.EXTRA_SKILL, skill)
            startActivity(intent)
        }

        val isMySelf = Utils.isMySelf(this, account, user)

        if (isMySelf) {
            actionButton.setImageResource(R.drawable.ic_action_edit)
        } else {
            actionButton.setImageResource(R.drawable.ic_action_chat)
        }

        val inflater = this@UserActivity.layoutInflater

        learningSkills.removeAllViews()
        user.learningSkills?.forEach { skill ->
            val view = Utils.inflateSkillItemView(this@UserActivity, inflater, skill, learningSkills)
            val skillButton = view.findViewById(R.id.skill_button)
            skillButton.tag = skill
            skillButton.setOnClickListener(skillOnClickListener)
            learningSkills.addView(view)
        }
        if (isMySelf) {
            learningLabel.setOnClickListener {
                val intent = Intent(this@UserActivity, SkillSelectorActivity::class.java)
                intent.putParcelableArrayListExtra(Constants.EXTRA_SKILLS, ArrayList(user.learningSkills))
                startActivityForResult(intent, REQUEST_SELECT_LEARNING_SKILLS)
            }
        } else {
            //TODO: Add empty view
        }
        masterSkills.removeAllViews()
        user.masterSkills?.forEach { skill ->
            val view = Utils.inflateSkillItemView(this@UserActivity, inflater, skill, masterSkills)
            val skillButton = view.findViewById(R.id.skill_button)
            skillButton.tag = skill
            skillButton.setOnClickListener(skillOnClickListener)
            masterSkills.addView(view)
        }
        if (isMySelf) {
            masterLabel.setOnClickListener {
                val intent = Intent(this@UserActivity, SkillSelectorActivity::class.java)
                intent.putParcelableArrayListExtra(Constants.EXTRA_SKILLS, ArrayList(user.learningSkills))
                startActivityForResult(intent, REQUEST_SELECT_MASTER_SKILLS)
            }
        } else {
            //TODO: Add empty view
        }
        val providers = user.providers
        providersContainer.removeAllViews()

        val websiteUrl = user.websiteUrl
        val hasWebsite = !TextUtils.isEmpty(websiteUrl)

        val providerOnClickListener = View.OnClickListener { v ->
            val provider = v.tag as Provider
            val intent: Intent
            if (provider.isSupported) {
                if (Provider.PROVIDER_BLOG == provider.name) {
                    // TODO open web address
                    return@OnClickListener
                }
                intent = Intent(this@UserActivity, ProviderContentActivity::class.java)
            } else if (isMySelf) {
                if (Provider.PROVIDER_BLOG == provider.name) {
                    // TODO open url editor
                    return@OnClickListener
                }
                intent = Intent(this@UserActivity, ProviderOAuthActivity::class.java)
            } else {
                return@OnClickListener
            }
            intent.putExtra(Constants.EXTRA_PROVIDER_NAME, provider.name)
            intent.putExtra(Constants.EXTRA_USER, user)
            intent.putExtra(Constants.EXTRA_ACCOUNT, account)
            startActivity(intent)
        }
        if (hasWebsite || isMySelf) {
            val provider = Provider("blog", hasWebsite)
            val view = Utils.inflateProviderItemView(this@UserActivity,
                    inflater, provider, providersContainer)
            view.tag = provider
            view.setOnClickListener(providerOnClickListener)
            providersContainer.addView(view)
        }
        if (providers != null) {
            for (provider in providers) {
                if (!provider.isSupported) continue
                val view = Utils.inflateProviderItemView(this@UserActivity,
                        inflater, provider, providersContainer)
                view.tag = provider
                view.setOnClickListener(providerOnClickListener)
                providersContainer.addView(view)
            }
            if (isMySelf) {
                for (provider in providers) {
                    if (provider.isSupported) continue
                    val view = Utils.inflateProviderItemView(this@UserActivity,
                            inflater, provider, providersContainer)
                    view.tag = provider
                    view.setOnClickListener(providerOnClickListener)
                    providersContainer.addView(view)
                }
            }
        }
        if (providersContainer.childCount > 0) {
            providersDivider.visibility = View.VISIBLE
        } else {
            providersDivider.visibility = View.GONE
        }
    }

    private fun updatePalette(palette: Palette) {
        val swatch = palette.darkVibrantSwatch ?: return
        toolbar.setTitleTextColor(swatch.titleTextColor)
        collapsingToolbar.setBackgroundColor(swatch.rgb)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.fab -> {
                if (Utils.isMySelf(this, account, currentUser)) {
                    val intent = Intent(this, ProfileEditorActivity::class.java)
                    intent.putExtra(Constants.EXTRA_ACCOUNT, account)
                    startActivity(intent)
                } else {
                    val intent = Intent(this, ChatActivity::class.java)
                    intent.putExtra(Constants.EXTRA_CONVERSATION, Conversation.fromUser(currentUser,
                            Utils.getAccountId(this, account)))
                    startActivity(intent)
                }
            }
            R.id.user_topics -> {
                val intent = Intent(this, UserTopicsActivity::class.java)
                intent.putExtra(Constants.EXTRA_ACCOUNT, account)
                intent.putExtra(Constants.EXTRA_USER, currentUser)
                startActivity(intent)
            }
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<TaskResponse<User>> {
        return UserLoader(this, account, currentUser.id)
    }

    override fun onLoadFinished(loader: Loader<TaskResponse<User>>, data: TaskResponse<User>) {
        if (data.hasData()) {
            val user = data.data
            val account = account
            val accountId = Utils.getAccountId(this, account)
            displayUser(user)
            if (StringUtils.equals(user.id, accountId)) {
                Utils.saveUserInfo(this@UserActivity, account, user)
            } else {
                TaskStarter.execute(object : AbstractTask<Any, Any, Any>() {
                    public override fun doLongOperation(o: Any): Any? {
                        val values = ContentValues()
                        values.put(Friendships.FRIEND, JsonSerializer.serialize(user))
                        val cr = contentResolver
                        val where = Expression.and(Expression.equalsArgs(Friendships.ACCOUNT_ID),
                                Expression.equalsArgs(Friendships.USER_ID)).sql
                        cr.update(Friendships.CONTENT_URI, values, where, arrayOf(accountId, user.id))
                        return null
                    }
                })
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_user, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val isMySelf = Utils.isMySelf(this, account, currentUser)
        menu.setMenuGroupAvailability(R.id.group_menu_friend, !isMySelf)
        menu.setMenuGroupAvailability(R.id.group_menu_myself, isMySelf)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> {
                Utils.openSettings(this, account)
                return true
            }
            R.id.share -> {
                val user = currentUser
                if (!TextUtils.isEmpty(user.username)) {
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "text/plain"
                    intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text_template,
                            Utils.getDisplayName(currentUser), Utils.getUserLink(user)))
                    startActivity(Intent.createChooser(intent, getString(R.string.share)))
                } else {
                    val df = SetUsernameDialogFragment()
                    df.show(supportFragmentManager, "set_username")
                }
                return true
            }
            R.id.block_user -> {
                TaskStarter.execute(object : AbstractTask<Any, Any, Any>() {
                    public override fun doLongOperation(o: Any): Any? {
                        val currentAccount = account
                        val yep = YepAPIFactory.getInstance(this@UserActivity, currentAccount)!!
                        try {
                            yep.blockUser(currentUser.id)
                        } catch (e: YepException) {
                            Log.w(Constants.LOGTAG, e)
                        }

                        return null
                    }
                })
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onLoaderReset(loader: Loader<TaskResponse<User>>) {

    }


    override val isTintBarEnabled: Boolean
        get() = false


    override fun onProfileUpdated(user: User) {
        displayUser(user)
    }

    class SetUsernameDialogFragment : DialogFragment(), DialogInterface.OnClickListener {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(R.string.set_username)
            builder.setNegativeButton(android.R.string.cancel, null)
            builder.setPositiveButton(android.R.string.ok, this)
            builder.setView(R.layout.dialog_set_username)
            return builder.create()
        }

        override fun onClick(dialog: DialogInterface, which: Int) {
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val editUsername = (dialog as Dialog).findViewById(R.id.edit_username) as EditText
                    (activity as UserActivity).setUsername(ParseUtils.parseString(editUsername.text))
                }
            }
        }
    }

    private fun setUsername(username: String) {
        val update = ProfileUpdate()
        update.setUsername(username)
        if (updateProfileTask == null || updateProfileTask!!.status != AsyncTask.Status.RUNNING) {
            updateProfileTask = UpdateProfileTask(this, account, update, null)
            updateProfileTask!!.execute()
        }
    }

    companion object {

        private val REQUEST_SELECT_MASTER_SKILLS = 111
        private val REQUEST_SELECT_LEARNING_SKILLS = 112
    }
}