/*
 * Copyright 2012 Google Inc. All Rights Reserved.
 * Copyright 2013 Yu AOKI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kieronquinn.app.smartspacer.utils.gmail

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.net.Uri
import android.text.TextUtils

/**
 *
 * Contract for use with the Gmail content provider.
 *
 *
 * Developers can use this content provider to display label information to the user.
 * <br></br>
 * The label information includes:
 *
 *  * Label name
 *  * Total number of conversations
 *  * Number of unread conversations
 *  * Label text color
 *  * Label background color
 *
 *
 *
 * This content provider is available in Gmail version 2.3.6 or newer for Froyo/Gingerbread
 * and version 4.0.5 and newer for Honeycomb and Ice Cream Sandwich
 *
 * An application can query the
 * [
 * Content Resolver](http://developer.android.com/reference/android/content/ContentResolver.html) directly
 * (or use a [Loader](http://developer.android.com/guide/topics/fundamentals/loaders.html))
 * to obtain a Cursor with information for all labels on an account
 * `Cursor labelsCursor = getContentResolver().query(GmailContract.Labels.getLabelsUri(
 * selectedAccount), null, null, null, null);`
 */
object GmailContract {
    /**
     * Permission required to access this [android.content.ContentProvider]
     */
    const val PERMISSION = "com.google.android.gm.permission.READ_CONTENT_PROVIDER"

    /**
     * Authority for the Gmail content provider.
     */
    const val AUTHORITY = "com.google.android.gm"
    const val LABELS_PARAM = "/labels"
    const val LABEL_PARAM = "/label/"
    const val BASE_URI_STRING = "content://$AUTHORITY"
    const val PACKAGE = "com.google.android.gm"

    /**
     * Check if the installed Gmail app supports querying for label information.
     *
     * @param c an application Context
     * @return true if it's safe to make label API queries
     */
    fun canReadLabels(c: Context): Boolean {
        var supported = false
        try {
            val info = c.packageManager.getPackageInfo(
                PACKAGE,
                PackageManager.GET_PROVIDERS or PackageManager.GET_PERMISSIONS
            )
            var allowRead = false
            val permissions = info.permissions
            if (permissions != null) {
                var i = 0
                val len = permissions.size
                while (i < len) {
                    val perm = permissions[i]
                    if (PERMISSION == perm.name && perm.protectionLevel < PermissionInfo.PROTECTION_SIGNATURE) {
                        allowRead = true
                        break
                    }
                    i++
                }
            }
            val providers = info.providers
            if (allowRead && providers != null) {
                var i = 0
                val len = providers.size
                while (i < len) {
                    val provider = providers[i]
                    if (AUTHORITY == provider.authority &&
                        TextUtils.equals(PERMISSION, provider.readPermission)
                    ) {
                        supported = true
                    }
                    i++
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            // Gmail app not found
        }
        return supported
    }

    /**
     * Table containing label information.
     */
    object Labels {
        /**
         * The MIME-type of uri providing a directory of
         * label items.
         */
        const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.google.android.gm.label"

        /**
         * The MIME-type of a label item.
         */
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.google.android.gm.label"
        const val _ID = "_id"

        /**
         * This string value is the canonical name of a label. Canonical names are not localized and
         * are not user-facing.
         *
         *
         * Type: TEXT
         */
        const val CANONICAL_NAME = "canonicalName"

        /**
         * This string value is the user-visible name of a label. Names of system labels
         * (Inbox, Sent, Drafts...) are localized.
         *
         *
         * Type: TEXT
         */
        const val NAME = "name"

        /**
         * This integer value is the number of conversations in this label.
         *
         *
         * Type: INTEGER
         */
        const val NUM_CONVERSATIONS = "numConversations"

        /**
         * This integer value is the number of unread conversations in this label.
         *
         *
         * Type: INTEGER
         */
        const val NUM_UNREAD_CONVERSATIONS = "numUnreadConversations"

        /**
         * This integer value is the label's foreground text color in 32-bit 0xAARRGGBB format.
         *
         *
         * Type: INTEGER
         */
        const val TEXT_COLOR = "text_color"

        /**
         * This integer value is the label's background color in 32-bit 0xAARRGGBB format.
         *
         *
         * Type: INTEGER
         */
        const val BACKGROUND_COLOR = "background_color"

        /**
         * This string column value is the uri that can be used in subsequent calls to
         * [android.content.ContentProvider.query] to query for information on the single
         * label represented by this row.
         *
         *
         * Type: TEXT
         */
        const val URI = "labelUri"

        /**
         * Returns a URI that, when queried, will return the list of labels for an
         * account.
         *
         *
         * To use the Labels API, an app must first find the email address of a
         * valid Gmail account to query for label information. The [AccountManager](http://developer.android.com/reference/android/accounts/AccountManager.html) can return this information ([example](https://developers.google.com/gmail/android)).
         *
         *
         * @param account Name of a valid Google account.
         * @return The URL that can be queried to retrieve the the label list.
         */
        fun getLabelsUri(account: String): Uri {
            return Uri.parse("$BASE_URI_STRING/$account$LABELS_PARAM")
        }

        /**
         * Label canonical names for default Gmail system labels.
         */
        object LabelCanonicalNames {
            /**
             * Canonical name for the Inbox label
             */
            const val CANONICAL_NAME_INBOX = "^i"

            /**
             * Canonical name for the Priority Inbox label
             */
            const val CANONICAL_NAME_PRIORITY_INBOX = "^iim"

            /**
             * Canonical name for the Starred label
             */
            const val CANONICAL_NAME_STARRED = "^t"

            /**
             * Canonical name for the Sent label
             */
            const val CANONICAL_NAME_SENT = "^f"

            /**
             * Canonical name for the Drafts label
             */
            const val CANONICAL_NAME_DRAFTS = "^r"

            /**
             * Canonical name for the All Mail label
             */
            const val CANONICAL_NAME_ALL_MAIL = "^all"

            /**
             * Canonical name for the Spam label
             */
            const val CANONICAL_NAME_SPAM = "^s"

            /**
             * Canonical name for the Trash label
             */
            const val CANONICAL_NAME_TRASH = "^k"
        }
    }
}