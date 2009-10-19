/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.core.domain.configuration;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

import java.util.Random;

/**
 * This class has tests for Configuration just like org.rhq.core.domain.configuration.test.ConfigurationTest. These
 * tests however are just plain, vanilla unit tests whereas the tests in org.rhq.core.domain.configuration.test.ConfigurationTest
 * are slower, longer running integration tests; hence, the separation.
 */
public class ConfigurationTest {

    @Test
    public void deepCopyWithoutProxiesShouldNotReturnReferenceToOriginalObject() {
        Configuration original = createConfiguration();
        Configuration copy = original.deepCopyWithoutProxies();

        assertNotSame(copy, original, "Expected a reference to a new Configuration object, not the original object being copied");
    }

    @Test
    public void deepCopyWithoutProxiesShouldCopySimpleFields() {
        Configuration original = createConfiguration();
        Configuration copy = original.deepCopyWithoutProxies();

        assertEquals(copy.getNotes(), original.getNotes(), "Failed to copy the notes property");
        assertEquals(copy.getVersion(), original.getVersion(), "Failed to copy version property");
    }

    @Test
    public void deepCopyWithoutProxiesShouldNotCopyIdProperty() {
        Configuration original = createConfiguration();
        Configuration copy = original.deepCopyWithoutProxies();

        assertFalse(copy.getId() == original.getId(), "The original id property should not be copied");
    }

    @Test
    public void deepCopyWithoutProxiesShouldCopyProperties() {
        Configuration original = createConfiguration();
        original.put(new PropertySimple("simpleProperty", "Simple Property"));

        Configuration copy = original.deepCopyWithoutProxies();

        assertNotSame(copy.getProperties(), original.getProperties(), "The properties property should not refer to the properties in the original object");
        assertEquals(copy.getProperties(), original.getProperties(), "Failed to copy the contents of the properties collection");
    }

    @Test
    public void deepCopyWithoutProxiesShouldNotReturnCopyReferenceOfOriginalProperty() {
        Configuration original = createConfiguration();
        String propertyName = "simpleProperty";
        original.put(new PropertySimple(propertyName, "Simple Property"));

        Configuration copy = original.deepCopyWithoutProxies();

        assertNotSame(copy.get(propertyName), original.get(propertyName), "Expected a refernce to a new property, not the original property being copied");
    }

    @Test
    public void deepCopyWithoutProxiesShouldSetParentReferenceOfCopiedProperties() {
        Configuration original = createConfiguration();
        String propertyName = "simpleProperty";
        original.put(new PropertySimple(propertyName, "Simple Property"));

        Configuration copy = original.deepCopyWithoutProxies();

        assertSame(
            copy.get(propertyName).getConfiguration(),
            copy,
            "The reference to the parent configuration should point to the newly copied configuration, not the original configuration"
        );
    }

    @Test
    public void equalsShouldBeFalseWhenArgumentIsNull() {
        Configuration config = new Configuration();

        assertFalse(config.equals(null), "equals() should be false for null argument.");
    }

    @Test
    public void equalsShouldBeFalseWhenArgumentIsNotAConfiguration() {
        Configuration config = new Configuration();

        assertFalse(config.equals(new Object()), "equals should return false when argument is not a " + Configuration.class.getSimpleName());
    }

    @Test
    public void equalsShouldBeReflexive() {
        Configuration config = new Configuration();

        assertTrue(config.equals(config), "equals() should be reflexive.");
    }

    @Test
    public void equalsAndHashCodeShouldBeSymmetricWhenBothConfigurationsAreEmpty() {
        Configuration c1 = new Configuration();
        Configuration c2 = new Configuration();

        assertTrue(c1.equals(c2) && c2.equals(c1), "equals() should be true and symmetric when both configs are empty.");
        assertEquals(c1.hashCode(), c2.hashCode(), "hashCodes should be the same when equals() returns true.");
    }

    @Test
    public void equalsAndHashCodeShouldBeSymmetricWhenConfigsHaveEqualStructuredAndNoRaw() {
        Configuration c1 = new Configuration();
        c1.put(new PropertySimple("foo", "bar"));

        Configuration c2 = new Configuration();
        c2.put(new PropertySimple("foo", "bar"));

        assertTrue(
            c1.equals(c2) && c2.equals(c1),
            "equals() should be true and symmetric when structured configs are equal and there are no raw configs."
        );
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void equalsAndHashCodeShouldBeSymmetricWhenConfigsHaveEqualRawAndNoStructured() {
        RawConfiguration rawConfig = createRawConfiguration("/tmp/foo");

        Configuration c1 = new Configuration();
        c1.addRawConfiguration(rawConfig);

        Configuration c2 = new Configuration();
        c2.addRawConfiguration(createCopyOfRawConfiguration(rawConfig));

        assertTrue(
            c1.equals(c2) && c2.equals(c1),
            "equals() should be true and symmetric when raw configs are equal and there are no structured configs."
        );
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    public void equalsAndHashCodeShouldBeTransitiveWhenConfigurationsAreEmpty() {
        Configuration c1 = new Configuration();
        Configuration c2 = new Configuration();
        Configuration c3 = new Configuration();

        assertTrue(c1.equals(c2) && c2.equals(c3), "equals() should be true when configs are empty.");
        assertTrue(c1.equals(c3), "equals should be transitive.");

        assertEquals(c1.hashCode(), c3.hashCode(), "hashCodes should be the same when equals returns true.");
    }

    @Test
    public void equalsAndHashCodeShouldBeTransitiveWhenConfigsHaveEqualStructuredAndNoRaw() {
        Configuration c1 = new Configuration();
        c1.put(new PropertySimple("foo", "bar"));

        Configuration c2 = new Configuration();
        c2.put(new PropertySimple("foo", "bar"));

        Configuration c3 = new Configuration();
        c3.put(new PropertySimple("foo", "bar"));

        assertTrue(c1.equals(c2) && c2.equals(c3), "equals() should be true when structured configs are equal and there are no raw configs.");
        assertTrue(c1.equals(c3), "equals should be transitive.");

        assertEquals(c1.hashCode(), c3.hashCode(), "hashCodes should be the same when equals() returns true.");
    }

    @Test
    public void equalsAndHashCodeShouldBeTransitiveWhenConfigsHaveEqualRawAndNoStructured() {
        RawConfiguration rawConfig = createRawConfiguration("/tmp/foo");

        Configuration c1 = new Configuration();
        c1.addRawConfiguration(rawConfig);

        Configuration c2 = new Configuration();
        c2.addRawConfiguration(createCopyOfRawConfiguration(rawConfig));

        Configuration c3 = new Configuration();
        c3.addRawConfiguration(createCopyOfRawConfiguration(rawConfig));

        assertTrue(c1.equals(c2) && c2.equals(c3), "equals() should be true when raw configs are equal and there are no structured configs.");
        assertTrue(c1.equals(c3), "equals should be transitive.");

        assertEquals(c1.hashCode(), c3.hashCode(), "hashCodes should be the same when equals() returns true.");
    }

    @Test
    public void equalsShouldBeFalseWhenOneConfigHasStructuredAndTheOtherDoesNot() {
        Configuration c1 = new Configuration();
        c1.put(new PropertySimple("foo", "bar"));

        Configuration c2 = new Configuration();

        assertFalse(c1.equals(c2), "equals() should be false when one config has structured and the other does not");
    }

    @Test
    public void equalsShouldBeFalseWhenBothHaveStructuredButNotRaw() {
        RawConfiguration rawConfig = createRawConfiguration("/tmp/foo");

        Configuration c1 = new Configuration();
        c1.put(new PropertySimple("foo", "bar"));
        c1.addRawConfiguration(rawConfig);

        Configuration c2 = new Configuration();
        c2.put(new PropertySimple("foo", "bar"));

        assertFalse(c1.equals(c2), "equals() should be false when one config has raw and the other does not.");
    }

    @Test
    public void equalsShouldBeFalseWhenBothHaveRawButNotStructured() {
        RawConfiguration r1 = createRawConfiguration("/tmp/foo");
        Configuration c1 = new Configuration();
        c1.addRawConfiguration(r1);
        c1.put(new PropertySimple("foo", "bar"));

        RawConfiguration r2 = createCopyOfRawConfiguration(r1);
        Configuration c2 = new Configuration();
        c2.addRawConfiguration(r2);

        assertFalse(c1.equals(c2), "equals() should be false when one config has structured and the other does not.");
    }

    @Test
    public void equalsShouldBeFalseWhenStructuredAreUnequalAndRawsAreEqual() {
        RawConfiguration rawConfig = createRawConfiguration("/tmp/foo");

        Configuration c1 = new Configuration();
        c1.put(new PropertySimple("foo", "bar"));
        c1.addRawConfiguration(rawConfig);

        Configuration c2 = new Configuration();
        c2.put(new PropertySimple("bar", "foo"));
        c2.addRawConfiguration(createCopyOfRawConfiguration(rawConfig));

        assertFalse(c1.equals(c2), "equals() should be false when structured configs are not equal.");
    }

    @Test
    public void equalsShouldBeFalseWhenStructuredAreEqualAndRawAreUnequal() {
        Configuration c1 = new Configuration();
        c1.put(new PropertySimple("foo", "bar"));
        c1.addRawConfiguration(createRawConfiguration("/tmp/foo"));

        Configuration c2 = new Configuration();
        c2.put(new PropertySimple("foo", "bar"));
        c2.addRawConfiguration(createRawConfiguration("/tmp/bar"));

        assertFalse(c1.equals(c2), "equals() should be false when raw configs are not equal.");
    }

    private Configuration createConfiguration() {
        Configuration config = new Configuration();
        config.setId(1);
        config.setNotes("notes");
        config.setVersion(1L);

        // make sure properties property is initialized
        config.getMap();

        return config;
    }

    private RawConfiguration createRawConfiguration(String path) {
        byte[] contents = new byte[10];

        Random random = new Random();
        random.nextBytes(contents);

        RawConfiguration rawConfig = new RawConfiguration();
        rawConfig.setPath(path);
        rawConfig.setContents(contents);

        return rawConfig;
    }

    private RawConfiguration createCopyOfRawConfiguration(RawConfiguration rawConfig) {
        RawConfiguration copy = new RawConfiguration();
        copy.setPath(rawConfig.getPath());
        copy.setContents(rawConfig.getContents());
        copy.setConfiguration(rawConfig.getConfiguration());

        return copy;
    }

}
