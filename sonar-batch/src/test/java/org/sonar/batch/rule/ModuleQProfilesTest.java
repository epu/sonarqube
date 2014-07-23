/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.rule;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.System2;
import org.sonar.batch.languages.DeprecatedLanguagesReferential;
import org.sonar.batch.languages.LanguagesReferential;
import org.sonar.batch.rules.DefaultQProfileReferential;
import org.sonar.batch.rules.QProfilesReferential;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.qualityprofile.db.QualityProfileDao;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class ModuleQProfilesTest extends AbstractDaoTestCase {

  LanguagesReferential languages = new DeprecatedLanguagesReferential(new Languages(new SimpleLanguage("java"), new SimpleLanguage("php")));
  Settings settings = new Settings();

  @Test
  public void find_profiles() throws Exception {
    // 4 profiles in db
    setupData("shared");
    QualityProfileDao dao = new QualityProfileDao(getMyBatis(), System2.INSTANCE);
    QProfilesReferential ref = new DefaultQProfileReferential(dao);

    settings.setProperty("sonar.profile.java", "Java One");
    settings.setProperty("sonar.profile.abap", "Abap One");
    settings.setProperty("sonar.profile.php", "Php One");

    ModuleQProfiles moduleQProfiles = new ModuleQProfiles(settings, languages, ref);
    List<QProfile> qProfiles = Lists.newArrayList(moduleQProfiles.findAll());

    // load only the profiles of languages detected in project
    assertThat(qProfiles).hasSize(2);
    assertThat(moduleQProfiles.findByLanguage("java")).isNotNull();
    assertThat(moduleQProfiles.findByLanguage("php")).isNotNull();
    assertThat(moduleQProfiles.findByLanguage("abap")).isNull();
    QProfile javaProfile = qProfiles.get(0);
    assertThat(javaProfile.getKey()).isEqualTo("java-one");
    assertThat(javaProfile.getName()).isEqualTo("Java One");
    assertThat(javaProfile.getLanguage()).isEqualTo("java");
    QProfile phpProfile = qProfiles.get(1);
    assertThat(phpProfile.getKey()).isEqualTo("php-one");
    assertThat(phpProfile.getName()).isEqualTo("Php One");
    assertThat(phpProfile.getLanguage()).isEqualTo("php");
  }

  @Test
  public void supported_deprecated_property() throws Exception {
    setupData("shared");
    QualityProfileDao dao = new QualityProfileDao(getMyBatis(), System2.INSTANCE);
    QProfilesReferential ref = new DefaultQProfileReferential(dao);

    // deprecated property
    settings.setProperty("sonar.profile", "Java Two");
    settings.setProperty("sonar.profile.php", "Php One");

    ModuleQProfiles moduleQProfiles = new ModuleQProfiles(settings, languages, ref);
    List<QProfile> qProfiles = Lists.newArrayList(moduleQProfiles.findAll());

    assertThat(qProfiles).hasSize(2);
    QProfile javaProfile = qProfiles.get(0);
    assertThat(javaProfile.getKey()).isEqualTo("java-two");
    assertThat(javaProfile.getName()).isEqualTo("Java Two");
    assertThat(javaProfile.getLanguage()).isEqualTo("java");

    // "Java Two" does not exist for PHP -> fallback to sonar.profile.php
    QProfile phpProfile = qProfiles.get(1);
    assertThat(phpProfile.getKey()).isEqualTo("php-one");
    assertThat(phpProfile.getName()).isEqualTo("Php One");
    assertThat(phpProfile.getLanguage()).isEqualTo("php");
  }

  @Test
  public void fail_if_unknown_profile() throws Exception {
    setupData("shared");
    QualityProfileDao dao = new QualityProfileDao(getMyBatis(), System2.INSTANCE);
    QProfilesReferential ref = new DefaultQProfileReferential(dao);

    settings.setProperty("sonar.profile.java", "Unknown");
    settings.setProperty("sonar.profile.php", "Php One");

    try {
      new ModuleQProfiles(settings, languages, ref);
      fail();
    } catch (MessageException e) {
      assertThat(e).hasMessage("Quality profile not found : 'Unknown' on language 'java'");
    }
  }

  private static class SimpleLanguage implements Language {

    private final String key;

    private SimpleLanguage(String key) {
      this.key = key;
    }

    @Override
    public String getKey() {
      return key;
    }

    @Override
    public String getName() {
      return key;
    }

    @Override
    public String[] getFileSuffixes() {
      return new String[0];
    }
  }
}