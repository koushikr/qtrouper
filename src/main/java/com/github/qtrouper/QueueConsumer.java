package com.github.qtrouper;

import com.github.qtrouper.core.models.QAccessInfo;
import com.github.qtrouper.core.models.QueueContext;

/**
 * @author koushik
 */
public abstract class QueueConsumer {

    public abstract boolean consume(QueueContext queueContext, QAccessInfo accessInfo) throws Exception;

    public abstract boolean consumeSideline(QueueContext queueContext, QAccessInfo accessInfo) throws Exception;

}
