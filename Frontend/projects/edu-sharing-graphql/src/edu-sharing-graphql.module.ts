import { Inject, NgModule } from '@angular/core';
import { HttpLink, Options } from 'apollo-angular/http';
import { Apollo, APOLLO_OPTIONS } from 'apollo-angular';
import { ApolloLink, InMemoryCache } from '@apollo/client/core';

@NgModule({
    declarations: [],
    imports: [],
    exports: [],
})
export class EduSharingGraphqlModule {
    constructor(apollo: Apollo, @Inject(APOLLO_OPTIONS) options: Options, httpLink: HttpLink) {
        const http = httpLink.create(options);
        const typenameMiddleware = new ApolloLink((operation, forward) => {
            if (operation.variables) {
                operation.variables = JSON.parse(
                    JSON.stringify(operation.variables),
                    EduSharingGraphqlModule.omitTypename,
                );
            }
            return forward(operation);
        });
        const myAppLink = ApolloLink.from([typenameMiddleware, http]);
        // remove the default injected client
        apollo.removeClient();
        // create a new one using the custom middleware
        apollo.create({
            link: myAppLink,
            cache: new InMemoryCache(),
        });
        // @TODO: check if this is feasible for resolving types of elements
        /*
        apollo.query({
            gql: `{
                  __schema {
                    types {
                      name
                      fields {
                        name
                        type {
                          kind
                          name
                          ofType {
                            kind
                            name
                            ofType {
                              kind
                              name
                            }
                          }
                        }
                      }
                    }
                  }
                }`
        });

         */
    }
    private static omitTypename(key: string, value: any) {
        return key === '__typename' ? undefined : value;
    }
}
