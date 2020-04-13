'use strict';

const slugify = require('slugify');

module.exports = {
  beforeSave: async model => {
    if (model.title && !model.slug) {
      model.slug = slugify(model.title).toLowerCase();
    }
  },
  beforeUpdate: async model => {
    // @todo as for now looks like this wont be needed, drop it later
    // if (model.getUpdate() && model.getUpdate().title) {
    //   model.update({
    //     slug: slugify(model.getUpdate().title),
    //   });
    // }
  },
};
