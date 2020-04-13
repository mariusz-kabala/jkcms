FROM node:12.16.1-alpine3.10

ARG GIT_HASH

ENV GIT_HASH ${GIT_HASH}

COPY . /app

WORKDIR /app

RUN yarn && yarn build

EXPOSE 1337

CMD ["yarn", "start"]
