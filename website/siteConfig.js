// See https://docusaurus.io/docs/site-config for all the possible
// site configuration options.

const repoUrl = 'https://github.com/disneystreaming/weaver-test';

const siteConfigJson = require("./siteConfig.json");

const siteConfig = {
  ...siteConfigJson,
  copyright: `Copyright © ${new Date().getFullYear()} Disney Streaming Services`,
}

module.exports = siteConfig;
