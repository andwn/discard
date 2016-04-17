package zone.pumpkinhill.discord4droid.util;

import java.util.Comparator;

import zone.pumpkinhill.discord4droid.handle.obj.Message;

/**
 * This is a comparator built to compare messages based on their timestamps
 */
public class MessageComparator implements Comparator<Message> {

    /**
     * The singleton instance of the reversed message comparator
     */
    public static final MessageComparator REVERSED = new MessageComparator(true);

    /**
     * The singleton instance of the default message comparator
     */
    public static final MessageComparator DEFAULT = new MessageComparator(false);

    private boolean reverse;

    public MessageComparator(boolean reverse) {
        this.reverse = reverse;
    }

    @Override
    public int compare(Message o1, Message o2) {
        return o1.equals(o2) ? 0 : (reverse ? -1 : 1) * o1.getTimestamp().compareTo(o2.getTimestamp());
    }
}
