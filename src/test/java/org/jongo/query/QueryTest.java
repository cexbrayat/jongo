/*
 * Copyright (C) 2011 Benoit GUEROUT <bguerout at gmail dot com> and Yves AMSELLEM <amsellem dot yves at gmail dot com>
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

package org.jongo.query;

import com.mongodb.DBObject;
import org.jongo.marshall.QueryMarshaller;
import org.jongo.marshall.jackson.JacksonQueryMarshaller;
import org.jongo.marshall.jackson.JsonEngine;
import org.junit.Test;

import static junit.framework.Assert.fail;
import static org.fest.assertions.Assertions.assertThat;

public class QueryTest {

    QueryMarshaller queryMarshaller = new JacksonQueryMarshaller(new JsonEngine());

    @Test
    public void shouldConvertToDBObject() throws Exception {

        Query query = new Query("{'value':1}", queryMarshaller);

        DBObject dbObject = query.toDBObject();

        assertThat(dbObject.containsField("value")).isTrue();
        assertThat(dbObject.get("value")).isEqualTo(1);
    }

    @Test
    public void shouldThrowExceptionOnInvalidQuery() throws Exception {

        try {
            new Query("{invalid}", queryMarshaller);
            fail();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalArgumentException.class);
            assertThat(e.getMessage()).contains("{invalid}");
        }
    }
}
