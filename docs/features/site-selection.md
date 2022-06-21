# Site Selection

The site selection screen allows the operator to select the site where they are working from.
This list is presented as a searchable dropdown field, so operators can type to search if there are many results. The search can be done on any part of the site name.

The icon on the top-right of the screen, in the action bar, enables operators to log out. They will be returned to the [login screen](/features/operator-login).

![Site Selection](../images/screenshots/site_selection.gif ':size=500')

## Default

Once a site has been selected, this remain pre-set in the dropdown field when this screen is visited again.
This makes it easier when the operator is logged out (due to session expiry), as he does not have to search the list again.
Tapping the dropdown field in this case will show the full lists of sites again.

## Downstream impact

The selected site will be used in participant registration and visit logging calls, to indicate where the action took place.
For participant matching, the results will also be split up according to whether they were registered at this site or not.

Furthermore, when registering a new participant, the telephone number country code will be defaulted to the country corresponding to this selected site.