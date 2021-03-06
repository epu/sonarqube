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
define([
  'workspace/views/viewer-header-view',
  'source-viewer/viewer',
  'templates/workspace'
], function (HeaderView, SourceViewer) {

  return Marionette.Layout.extend({
    className: 'workspace-viewer',
    template: Templates['workspace-viewer'],

    regions: {
      headerRegion: '.workspace-viewer-header',
      viewerRegion: '.workspace-viewer-container'
    },

    onRender: function () {
      this.showHeader();
      this.showViewer();
    },

    showHeader: function () {
      var headerView = new HeaderView({ model: this.model });
      this.headerRegion.show(headerView);
    },

    showViewer: function () {
      if (SourceViewer == null) {
        SourceViewer = require('source-viewer/viewer');
      }
      var that = this,
          viewer = new SourceViewer(),
          options = this.model.toJSON();
      viewer.open(this.model.id);
      viewer.on('loaded', function () {
        that.model.set(viewer.model.toJSON());
        if (options.line != null) {
          viewer.highlightLine(options.line);
          viewer.scrollToLine(options.line);
        }
      });
      this.viewerRegion.show(viewer);
    }
  });

});
