package me.ccrama.redditslide;

import net.dean.jraw.models.PrivateMessage;
import net.dean.jraw.models.Submission;

import java.util.ArrayList;
import java.util.List;

import me.ccrama.redditslide.Adapters.CommentObject;
import me.ccrama.redditslide.Adapters.SubredditPosts;

/**
 * Created by ccrama on 9/19/2015.
 */
public class DataShare {
    public static List<Submission> sharedSubreddit;
    public static Submission sharedSubmission;
    //   public static Submission notifs;
    public static PrivateMessage sharedMessage;
    public static ArrayList<CommentObject> sharedComments;
    public static String subAuthor;
    public static SubredditPosts sharedSub;

}
