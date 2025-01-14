/*
 * SonarQube :: Plugins :: SCM :: Perforce
 * Copyright (C) 2014-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.scm.perforce;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileAnnotation;
import com.perforce.p4java.core.file.IFileRevisionData;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.option.server.GetFileAnnotationsOptions;
import com.perforce.p4java.option.server.GetRevisionHistoryOptions;
import com.perforce.p4java.server.IOptionsServer;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.scm.BlameCommand.BlameOutput;
import org.sonar.api.batch.scm.BlameLine;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

public class PerforceBlameCommandTest {

  @Test
  public void testBlameUnSubmittedFile() throws Exception {
    BlameOutput blameOutput = mock(BlameOutput.class);
    IOptionsServer server = mock(IOptionsServer.class);
    PerforceBlameCommand command = new PerforceBlameCommand(mock(PerforceConfiguration.class));

    IFileAnnotation annotation = mock(IFileAnnotation.class);
    when(annotation.getDepotPath()).thenReturn(null);

    when(server.getFileAnnotations(anyList(), any(GetFileAnnotationsOptions.class))).thenReturn(Collections.singletonList(annotation));

    command.blame(mock(InputFile.class), server, blameOutput);

    verifyNoInteractions(blameOutput);
  }

  @Test
  public void testBlameSubmittedFile() throws Exception {
    BlameOutput blameOutput = mock(BlameOutput.class);

    IOptionsServer server = mock(IOptionsServer.class);
    PerforceBlameCommand command = new PerforceBlameCommand(mock(PerforceConfiguration.class));

    // Changelist 3 is present in history

    IFileAnnotation line1ChangeList3 = mock(IFileAnnotation.class);
    when(line1ChangeList3.getDepotPath()).thenReturn("foo/bar/src/Foo.java");
    when(line1ChangeList3.getLower()).thenReturn(3);
    when(line1ChangeList3.getLine()).thenReturn("line1");

    IFileAnnotation line2ChangeList3 = mock(IFileAnnotation.class);
    when(line2ChangeList3.getDepotPath()).thenReturn("foo/bar/src/Foo.java");
    when(line2ChangeList3.getLower()).thenReturn(3);
    when(line2ChangeList3.getLine()).thenReturn("line2");

    // Changelist 4 is not present in history but can be fetched from server

    IFileAnnotation line3ChangeList4 = mock(IFileAnnotation.class);
    when(line3ChangeList4.getDepotPath()).thenReturn("foo/bar/src/Foo.java");
    when(line3ChangeList4.getLower()).thenReturn(4);
    when(line3ChangeList4.getLine()).thenReturn("line3");

    // Changelist 5 is not present in history nor in server

    IFileAnnotation line4ChangeList5 = mock(IFileAnnotation.class);
    when(line4ChangeList5.getDepotPath()).thenReturn("foo/bar/src/Foo.java");
    when(line4ChangeList5.getLower()).thenReturn(5);
    when(line4ChangeList5.getLine()).thenReturn("line4");

    // Put Changlist 4 again to verify we fetch only once from server
    IFileAnnotation line5ChangeList4 = mock(IFileAnnotation.class);
    when(line5ChangeList4.getDepotPath()).thenReturn("foo/bar/src/Foo.java");
    when(line5ChangeList4.getLower()).thenReturn(4);
    when(line5ChangeList4.getLine()).thenReturn("line5");

    Map<IFileSpec, List<IFileRevisionData>> result = new HashMap<>();
    IFileSpec fileSpecResult = mock(IFileSpec.class);
    when(fileSpecResult.getOpStatus()).thenReturn(FileSpecOpStatus.VALID);
    IFileRevisionData revision3 = mock(IFileRevisionData.class);
    when(revision3.getChangelistId()).thenReturn(3);
    Date date = new Date();
    when(revision3.getDate()).thenReturn(date);
    when(revision3.getUserName()).thenReturn("jhenry");
    result.put(fileSpecResult, Collections.singletonList(revision3));

    when(server.getRevisionHistory(anyList(), any(GetRevisionHistoryOptions.class))).thenReturn(result);
    when(server.getFileAnnotations(anyList(), any(GetFileAnnotationsOptions.class)))

            .thenReturn(Arrays.asList(line1ChangeList3, line2ChangeList3, line3ChangeList4, line4ChangeList5, line5ChangeList4));

    IChangelist changelist = mock(IChangelist.class);
    when(changelist.getDate()).thenReturn(date);
    when(changelist.getUsername()).thenReturn("bgates");
    when(server.getChangelist(4)).thenReturn(changelist);

    when(server.getChangelist(5)).thenReturn(null);

    InputFile inputFile = mock(InputFile.class);
    command.blame(inputFile, server, blameOutput);

    BlameLine line1 = new BlameLine().revision("3").date(date).author("jhenry");
    BlameLine line2 = new BlameLine().revision("3").date(date).author("jhenry");
    BlameLine line3 = new BlameLine().revision("4").date(date).author("bgates");
    BlameLine line4 = new BlameLine().revision("5").date(new Date(0)).author("unknown");
    BlameLine line5 = new BlameLine().revision("4").date(date).author("bgates");
    verify(blameOutput).blameResult(inputFile, Arrays.asList(line1, line2, line3, line4, line5));

    // Changelist 4 should have been fetched only once
    verify(server, times(1)).getChangelist(4);
  }

  @Test
  public void testBlameSubmittedFileLastEmptyLine() throws Exception {
    BlameOutput blameOutput = mock(BlameOutput.class);
    IOptionsServer server = mock(IOptionsServer.class);
    PerforceBlameCommand command = new PerforceBlameCommand(mock(PerforceConfiguration.class));

    IFileAnnotation annotation = mock(IFileAnnotation.class);
    when(annotation.getDepotPath()).thenReturn("foo/bar/src/Foo.java");
    when(annotation.getLower()).thenReturn(3);

    when(server.getFileAnnotations(anyList(), any(GetFileAnnotationsOptions.class))).thenReturn(Collections.singletonList(annotation));

    IChangelist changelist = mock(IChangelist.class);
    Date date = new Date();
    when(changelist.getDate()).thenReturn(date);
    when(changelist.getUsername()).thenReturn("jhenry");
    when(server.getChangelist(3)).thenReturn(changelist);

    InputFile inputFile = mock(InputFile.class);
    when(inputFile.lines()).thenReturn(2);
    command.blame(inputFile, server, blameOutput);

    BlameLine line = new BlameLine().revision("3").date(date).author("jhenry");

    verify(blameOutput).blameResult(inputFile, Arrays.asList(line, line));
  }

  @Test
  public void testRetryBlameThrowsAfterRetriesExhausted() throws Exception {
    BlameOutput blameOutput = mock(BlameOutput.class);
    IOptionsServer server = mock(IOptionsServer.class);
    InputFile inputFile = mock(InputFile.class);
    PerforceBlameCommand command = new PerforceBlameCommand(mock(PerforceConfiguration.class));

    doThrow(new P4JavaException()).when(server).getFileAnnotations(anyList(), any(GetFileAnnotationsOptions.class));
    try {
      command.tryBlame(inputFile, server, blameOutput);
      fail("Exception should have been re-thrown.");
    } catch (P4JavaException e) {
      verify(server, times(PerforceBlameCommand.MAX_ATTEMPTS)).getFileAnnotations(anyList(), any(GetFileAnnotationsOptions.class));
    }
  }

  @Test
  public void testDegradeAnnotationOptionsForRetry() throws Exception {
    BlameOutput blameOutput = mock(BlameOutput.class);
    IOptionsServer server = mock(IOptionsServer.class);
    InputFile inputFile = mock(InputFile.class);
    PerforceBlameCommand command = new PerforceBlameCommand(mock(PerforceConfiguration.class));
    when(server.getFileAnnotations(anyList(), argThat(isFollowIntegrations(true))))
            .thenThrow(new P4JavaException());
    when(server.getFileAnnotations(anyList(), argThat(isFollowIntegrations(false))))
            .thenReturn(Collections.emptyList());
    command.tryBlame(inputFile, server, blameOutput);
  }

  private Matcher<GetFileAnnotationsOptions> isFollowIntegrations(final boolean follow) {
    return new BaseMatcher<GetFileAnnotationsOptions>() {
      @Override
      public boolean matches(Object o) {
        return o instanceof GetFileAnnotationsOptions
                && ((GetFileAnnotationsOptions) o).isFollowAllIntegrations() == follow;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("matches follow integration annotation option");
      }
    };
  }

}
