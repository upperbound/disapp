package com.disapp.annotations;

import com.disapp.BotRunner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Определяет класс, который будет инициализирован с помощью {@link BotRunner#initialize()}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface InitClass {}
