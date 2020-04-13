const slugify = require("slugify");

module.exports = (strapi) => {
  return {
    initialize: function () {
      strapi.app.use(async (ctx, next) => {
        if (
          ctx.method === "POST" &&
          ctx.url.includes("application::articles.articles")
        ) {
          const { data } = ctx.request.body;
          const json = JSON.parse(data);
          const slug = slugify(json.title).toLowerCase();

          const count = await strapi.query("articles").count({
            slug,
            language: json.language,
          });

          if (count > 0) {
            return ctx.badRequest(null, [
              {
                messages: [
                  {
                    id: `Article with slug ${slug} already exists`,
                  },
                ],
              },
            ]);
          }
        }
        await next();
      });
    },
  };
};
