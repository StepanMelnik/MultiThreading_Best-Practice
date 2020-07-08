package com.sme.multithreading.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Represents immutable POJO with message properties.
 */
public final class Message
{
    private final int id;
    private final int delay;
    private final String message;

    public Message(int id, int delay, String message)
    {
        this.id = id;
        this.delay = delay;
        this.message = message;
    }

    public int getId()
    {
        return id;
    }

    public int getDelay()
    {
        return delay;
    }

    public String getMessage()
    {
        return message;
    }

    @Override
    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj)
    {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}
